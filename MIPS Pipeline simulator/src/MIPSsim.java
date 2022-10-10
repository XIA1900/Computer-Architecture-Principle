/*On my honor, I have neither given nor received unauthorized aid on this assignment
* ufid: 28267331
* Name: Yuwei Xia
* */
import java.io.*;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;

public class MIPSsim {
    static String input, output_disassembly, output_simulation;
    static HashMap<String, String> category[];
    static int[] registers;
    static HashMap<Integer, Integer> data;
    static HashMap<Integer, String> instructions;
    static int start;
    static int data_start;

    static LinkedList<String>[] buf;
    static final int buf1 = 8, buf2 = 2, buf3 = 2, buf4 = 2;
    static int[] registers_state;
    static int[] branch_raw;
    
    static boolean[] structure_free; //alu1, alu2, mul1, mul2, mul3, mem, wb1, wb2, wb3;

    static String waiting;
    static String executed;

    public static void main(String args[]) {

        input = args[0];
        //input = new String("sample.txt");
        output_disassembly = "disassembly.txt";
        output_simulation = "simulation.txt";

        category = new HashMap[3];
        for(int i=0; i<3; i++) category[i] = new HashMap();
        category[0].put("000","J");
        category[0].put("001","BEQ");
        category[0].put("010","BNE");
        category[0].put("011","BGTZ");
        category[0].put("100","SW");
        category[0].put("101","LW");
        category[0].put("110","BREAK");
        category[1].put("000","ADD");
        category[1].put("001","SUB");
        category[1].put("010","AND");
        category[1].put("011","OR");
        category[1].put("100","SRL");
        category[1].put("101","SRA");
        category[1].put("110","MUL");
        category[2].put("000","ADDI");
        category[2].put("001","ANDI");
        category[2].put("010","ORI");


        registers = new int[32];
        data = new HashMap();
        instructions = new HashMap();
        start = 260;
        data_start = -1;

        buf = new LinkedList[10];
        for(int j=0; j<10; j++) buf[j] = new LinkedList<>();

        registers_state = new int[32];
        structure_free = new boolean[9];
        Arrays.fill(structure_free, true);
        branch_raw = new int[32];

        waiting = "";
        executed = "";

        disassembler();
        simulator();

    }


    public static void disassembler() {
        try {
            File filename = new File(input);
            InputStreamReader rdr = new InputStreamReader(new FileInputStream(filename));
            BufferedReader br = new BufferedReader(rdr);

            File writename = new File(output_disassembly);
            writename.createNewFile();
            BufferedWriter out = new BufferedWriter(new FileWriter(writename));
            String bits = "";
            bits = br.readLine();

            int pos = start;
            //deal with instructions until BREAK
            while(bits != null) {
                out.write(bits+"\t"+ pos + "\t");
                pos += 4;
                String cate = bits.substring(0,3);
                String opcode = bits.substring(3,6);
                String operation = "";
                if(cate.equals("000")) {
                    operation = category[0].get(opcode) + " ";
                    if(opcode.equals("001") || opcode.equals("010") || opcode.equals("011")) {  //BEQ, BNE, BGTZ
                        String rs = bits.substring(6,11);
                        String rt = bits.substring(11,16);
                        String offset = bits.substring(16,32);
                        offset = offset + "00";
                        if(offset.charAt(0) == '1') offset = "11111111111111" + offset;  //neg
                        if(!opcode.equals("011")) operation += "R" + Integer.parseInt(rs, 2) + ", " + "R" + Integer.parseInt(rt, 2) + ", #" + Integer.parseUnsignedInt(offset, 2);
                        else  operation += "R" + Integer.parseInt(rs, 2) + ", #" + Integer.parseUnsignedInt(offset, 2);
                    }
                    else if(opcode.equals("000")) {  //J, unsigned
                        String index = bits.substring(6,32);
                        String next_address = Integer.toBinaryString(pos);
                        StringBuilder supple = new StringBuilder();
                        while(supple.length() < 32 - next_address.length()) supple.append("0") ;
                        next_address = supple + next_address;
                        index = next_address.substring(0,4) + index + "00";   //effective address;
                        operation += "#" + Long.parseUnsignedLong(index, 2);
                    }
                    else if(opcode.equals("110")){ //BREAK
                        out.write(operation.substring(0,5) + "\r\n");
                        instructions.put(pos-4, operation.substring(0,5));
                        break;
                    }
                    else { //LW, SW
                        String base = bits.substring(6,11);
                        String rt = bits.substring(11,16);
                        String offset = bits.substring(16,32);
                        if(offset.charAt(0) == '1') offset = "1111111111111111" + offset;
                        operation += "R" + Integer.parseInt(rt,2) + ", " + Integer.parseInt(offset,2) + "(R" + Integer.parseUnsignedInt(base,2) + ")";
                    }
                }
                else if(cate.equals("001")) {
                    operation = category[1].get(opcode) + " ";
                    String dest = bits.substring(6,11);  //register
                    String src1 = bits.substring(11,16);  //register
                    operation += "R" + Integer.parseInt(dest, 2) + ", " + "R" + Integer.parseInt(src1, 2) + ", ";
                    String src2 = bits.substring(16,21); //can be immediate or register
                    String temp = String.valueOf(Integer.parseInt(src2, 2));
                    if(opcode.equals("100") || opcode.equals("101")) {
                        operation += "#" + temp;   //SRL, SRA
                    }
                    else {
                        //temp = String.valueOf(Integer.parseInt(src2, 2));
                        operation += "R" + temp;
                    }
                }
                else {
                    operation = category[2].get(opcode) + " ";
                    String dest = bits.substring(6,11); //register
                    String src1 = bits.substring(11,16); //register
                    String value = bits.substring(16,32); //immediate
                    if(opcode.equals("000")) { //ADD, Signed
                        if(value.charAt(0) == '1')  //neg
                            value = "1111111111111111" + value;
                    }
                    operation += "R" + Integer.parseInt(dest, 2) + ", " + "R" + Integer.parseInt(src1, 2) + ", #" + Integer.parseUnsignedInt(value, 2);

                }
                instructions.put(pos-4, operation);
                out.write(operation + "\r\n");
                bits = br.readLine();
            }

            data_start = pos;
            bits = br.readLine();
            while(bits != null) {
                out.write(bits+"\t"+pos+"\t");
                int number = 0;
                number = Integer.parseUnsignedInt(bits,2);
                data.put(pos, number);
                out.write(number+"\r\n");
                pos += 4;
                bits = br.readLine();
            }
            out.flush();
            out.close();
        }
        catch(Exception e) {
            e.printStackTrace();
        }
    }


    public static void simulator() {
        int pc = start;
        int cycle = 1;

        try {
            File writename2 = new File(output_simulation);
            writename2.createNewFile();
            BufferedWriter out2 = new BufferedWriter(new FileWriter(writename2));
            while(!executed.equals("[BREAK]")) {
                int fetch = 0, alu1 = 0, alu2 = 0, mul1 = 0, mem = 0, mul2 = 0, mul3 = 0, wb1 = 0, wb2 = 0, wb3 = 0;
                //Instruction Fetch:
                if(waiting.equals("")) {   //no branch waiting
                    executed = "";
                    if(buf[0].size()< buf1) {
                        int slot = Math.min(buf1 - buf[0].size(), 4);

                        while(slot > 0) {

                            String inst = instructions.get(pc);
                            if(inst.equals("BREAK")) {
                                executed = "[" + inst +"]";
                                break;
                            }

                            String operation = inst.substring(0, inst.indexOf(' '));
                            if(operation.equals("J")){
                                pc = jump(inst, pc);
                                break;
                            }
                            else if(operation.equals("BEQ")) {
                                pc = beq(inst, pc);
                                break;
                            }
                            else if(operation.equals("BNE")) {
                                pc = bne(inst, pc);
                                break;
                            }
                            else if(operation.equals("BGTZ")) {
                                pc = bgtz(inst, pc);
                                break;
                            }
                            else {
                                buf[0].offer(inst);
                                pc += 4;
                                fetch ++;
                                slot--;
                                String oper = inst.substring(0, inst.indexOf(" "));
                                String[] operands = inst.substring(inst.indexOf(' ')+1).split(", ");
                                int r1, r2, r3;
                                if(!oper.equals("SW") ) {
                                    r1 = Integer.valueOf(operands[0].substring(1));
                                    branch_raw[r1] = 1;
                                }
                            }
                        }

                    }
                    else{ //stall
                    }
                }

                //Issue Unit: check buf1

                if(buf[0].size() > fetch) {  //buf[0] has instructions at the beginning of this cycle
                    int space1 = Math.min(buf2, buf2 - buf[1].size());
                    int space2 = Math.min(buf3, buf3 - buf[2].size());
                    int space3 = Math.min(buf4, buf4 - buf[3].size());
                    int output = 0;

                    int id = 0;
                    int unissued_stores = 0;

                    while(id < buf[0].size() && output < 6) {
                        String inst = buf[0].get(id);
                        String operation = inst.substring(0, inst.indexOf(" "));
                        String[] operands = inst.substring(inst.indexOf(' ')+1).split(", ");
                        int r1, r2, r3;

                        if(operation.equals("LW") || operation.equals("SW")) {
                            r1 = Integer.valueOf(operands[0].substring(1));
                            r2 = Integer.valueOf(operands[1].substring(operands[1].indexOf("(")+2,operands[1].indexOf(")")));
                            if(unissued_stores == 0 && space1 > 0) {
                                if(operation.equals("SW")) {
                                    if(registers_state[r1] < 2 && registers_state[r2] < 2) {
                                        buf[0].remove(id);
                                        buf[1].offer(inst);
                                        registers_state[r1] = 3;
                                        registers_state[r2] = 3;
                                        id--;
                                        alu1++;
                                        output++;
                                        space1--;
                                    }
                                    else if(registers_state[r1] == 2 && registers_state[r2] != 2) {
                                        registers_state[r2] = 1;
                                        unissued_stores++;
                                    }
                                    else if(registers_state[r2] == 2 && registers_state[r1] != 2) {
                                        registers_state[r1] = 1;
                                        unissued_stores++;
                                    }
                                }
                                else {  //LW
                                    if(registers_state[r1] == 0 && registers_state[r2] < 2) {
                                        registers_state[r2] = 1;
                                        registers_state[r1] = 2;
                                        buf[0].remove(id);
                                        buf[1].offer(inst);
                                        id--;
                                        alu1++;
                                        output++;
                                        space1--;
                                    }
                                    else {
                                        if(registers_state[r2] < 2) {
                                            registers_state[r2] = 1;
                                            if(registers_state[r1] == 1) registers_state[r1] = 4;
                                        }
                                        else {
                                            if(registers_state[r1] == 0) registers_state[r1] = 3;
                                            else if(registers_state[r1] == 1) registers_state[r1] = 4;
                                        }
                                    }
                                }
                            }
                        }
                        else {
                            r1 = Integer.valueOf(operands[0].substring(1));
                            r2 = Integer.valueOf(operands[1].substring(1));
                            r3 = Integer.valueOf(operands[2].substring(1));
                            if(operation.equals("MUL") && space3 > 0) {
                                //R1 is not used and R2 R3 are not used or only read
                                if(registers_state[r1] == 0 && registers_state[r2] < 2 && registers_state[r3] < 2) {
                                    registers_state[r2] = 1;  // read
                                    registers_state[r3] = 1;  // read
                                    registers_state[r1] = 2;  // write
                                    buf[0].remove(id);
                                    buf[3].offer(inst);
                                    id--;
                                    mul1++;
                                    output++;
                                    space3--;
                                }
                                else {
                                    if(registers_state[r2] == 0) registers_state[r2] = 1; //0 means not used
                                    if(registers_state[r3] == 0) registers_state[r3] = 1;

                                    if(registers_state[r1] == 1) registers_state[r1] = 4; //r1 is WAR
                                    else if(registers_state[r1] == 0) registers_state[r1] = 3;
                                }
                            }
                            else if((operation.equals("ADD") || operation.equals("SUB") || operation.equals("AND") || operation.equals("OR")) && space2 > 0) {
                                if (registers_state[r1] == 0 && registers_state[r2] < 2 && registers_state[r3] < 2) {
                                    registers_state[r2] = 1;  //
                                    registers_state[r3] = 1;  //
                                    registers_state[r1] = 2;
                                    buf[2].offer(inst);
                                    buf[0].remove(id);
                                    id--;
                                    alu2++;
                                    output++;
                                    space2--;
                                }
                                else {
                                    if(registers_state[r2] == 0) registers_state[r2] = 1;
                                    if(registers_state[r3] == 0) registers_state[r3] = 1;
                                    if(registers_state[r1] == 1) registers_state[r1] = 4;
                                    else if(registers_state[r1] == 0) registers_state[r1] = 3;
                                }
                            }
                            else if((operation.equals("SRL") || operation.equals("SRA") || operation.equals("ADDI") || operation.equals("ANDI") || operation.equals("ORI")) && space2 > 0) {
                                if ((registers_state[r1] == 0) && registers_state[r2] < 2) {      //deleted here
                                    registers_state[r2] = 1;  //
                                    registers_state[r1] = 2;
                                    buf[2].offer(inst);
                                    buf[0].remove(id);
                                    id--;
                                    alu2++;
                                    output++;
                                    space2--;
                                }
                                else {
                                    if(registers_state[r2] == 0) registers_state[r2] = 1;

                                    if(registers_state[r1] == 1) registers_state[r1] = 4;
                                    else if(registers_state[r1] == 0) registers_state[r1] = 3;
                                }
                            }
                        }
                        id++;
                    }

                }

                if(!waiting.equals("")) {
                    String inst = waiting.substring(1,waiting.length()-1);
                    String operation = inst.substring(0, inst.indexOf(' '));
                    if(operation.equals("BEQ")) pc = beq(inst, pc);
                    else if(operation.equals("BNE")) pc = bne(inst, pc);
                    else if(operation.equals("BGTZ")) pc = bgtz(inst, pc);
                }

                //ALU1
                if(buf[1].size() > alu1) {
                    String inst = buf[1].poll();
                    buf[4].offer(inst);
                    mem++;

                }


                //MEM
                if(buf[4].size() > mem) { //buf[4] has instruction in this cycle
                    String inst = buf[4].poll();
                    String operation = inst.substring(0, inst.indexOf(' '));
                    String[] operands = inst.substring(inst.indexOf(' ')+1).split(", ");
                    int r1, r2, ofs;

                    r1 = Integer.valueOf(operands[0].substring(1));
                    r2 = Integer.valueOf(operands[1].substring(operands[1].indexOf("(")+2,operands[1].indexOf(")")));
                    ofs = Integer.valueOf(operands[1].substring(0, operands[1].indexOf("(")));
                    if(operation.equals("LW")) {
                        buf[7].offer("[" + data.get(registers[r2]+ofs) + ", R" + r1 + "]" + " " + r2);
                        wb1++;

                    }
                    else {
                        data.put(ofs+registers[r2], registers[r1]);
                    }
                }

                //ALU2
                if(buf[2].size() > alu2) {
                    String inst = buf[2].poll();
                    String operation = inst.substring(0, inst.indexOf(' '));
                    String[] operands = inst.substring(inst.indexOf(' ')+1).split(", ");
                    int r1, r2, r3, imdv, result = 0;

                    r1 = Integer.valueOf(operands[0].substring(1));
                    r2 = Integer.valueOf(operands[1].substring(1));
                    r3 = Integer.valueOf(operands[2].substring(1));
                    imdv = Integer.valueOf(operands[2].substring(1));

                    switch(operation) {
                        case "ADD":
                            result = registers[r2] + registers[r3];
                            buf[5].offer("[" + result + ", R" + r1 + "]" + " " + r2 + " " + r3);
                            break;
                        case "SUB":
                            result = registers[r2] - registers[r3];
                            buf[5].offer("[" + result + ", R" + r1 + "]" + " " + r2 + " " + r3);
                            break;
                        case "AND":
                            result = registers[r2] & registers[r3];
                            buf[5].offer("[" + result + ", R" + r1 + "]" + " " + r2 + " " + r3);
                            break;
                        case "OR":
                            result = registers[r2] | registers[r3];
                            buf[5].offer("[" + result + ", R" + r1 + "]" + " " + r2 + " " + r3);
                            break;
                        case "SRL":
                            result = registers[r2] >>> imdv;
                            buf[5].offer("[" + result + ", R" + r1 + "]" + " " + r2);
                            break;
                        case "SRA":
                            result = registers[r2] >> imdv;
                            buf[5].offer("[" + result + ", R" + r1 + "]" + " " + r2);
                            break;
                        case "ADDI":
                            result = registers[r2] + imdv;
                            buf[5].offer("[" + result + ", R" + r1 + "]" + " " + r2);
                            break;
                        case "ANDI":
                            result = registers[r2] & imdv;
                            buf[5].offer("[" + result + ", R" + r1 + "]" + " " + r2);
                            break;
                        case "ORI":
                            result = registers[r2] | imdv;
                            buf[5].offer("[" + result + ", R" + r1 + "]" + " " + r2);
                            break;
                    }
                    wb2++;
                }

                //MUL1
                if(buf[3].size() > mul1) {
                    buf[6].offer(buf[3].poll());
                    mul2++;
                }

                //MUL2
                if(buf[6].size() > mul2) {
                    buf[8].offer(buf[6].poll());
                    mul3++;
                }

                //MUL3
                if(buf[8].size() > mul3) {
                    String inst = buf[8].poll();
                    String[] operands = inst.substring(inst.indexOf(' ')+1).split(", ");
                    int r1, r2, r3, result;

                    r1 = Integer.valueOf(operands[0].substring(1));
                    r2 = Integer.valueOf(operands[1].substring(1));
                    r3 = Integer.valueOf(operands[2].substring(1));
                    Long tp = (long)registers[r2] * (long)registers[r3];
                    String tmp = Long.toBinaryString(tp);
                    if(tmp.length()>32) result= Integer.parseUnsignedInt(tmp.substring(tmp.length()-32),2);
                    else result = Integer.parseUnsignedInt(tmp,2);

                    buf[9].offer("[" + result + ", R" + r1 + "]" + " " + r2 + " " + r3);
                    wb3++;
                }

                //WB
                if(buf[7].size() > wb1) {
                    String inst = buf[7].poll();
                    String first_part = inst.substring(1, inst.indexOf(']'));
                    String second_part = inst.substring(inst.indexOf(']')+2);
                    String[] ops = first_part.split(", ");
                    int src = Integer.parseInt(second_part);
                    int dest = Integer.valueOf(ops[1].substring(1));
                    int res = Integer.valueOf(ops[0]);
                    registers[dest] = res;
                    registers_state[dest] = 0;
                    registers_state[src] = 0;
                    branch_raw[dest] = 0;
                }

                if(buf[5].size() > wb2) {
                    String inst = buf[5].poll();
                    String first_part = inst.substring(1, inst.indexOf(']'));
                    String[] ops = first_part.split(", ");
                    int dest = Integer.valueOf(ops[1].substring(1));
                    int res = Integer.valueOf(ops[0]);
                    registers[dest] = res;
                    registers_state[dest] = 0;
                    branch_raw[dest] = 0;
                }

                if(buf[9].size() > wb3) {
                    String inst = buf[9].poll();
                    String first_part = inst.substring(1, inst.indexOf(']'));
                    String[] ops = first_part.split(", ");
                    int dest = Integer.valueOf(ops[1].substring(1));
                    int res = Integer.valueOf(ops[0]);
                    registers[dest] = res;
                    registers_state[dest] = 0;
                    branch_raw[dest] = 0;
                }

                //output:
                output(out2, cycle);
                for(int k=0; k<32; k++) {
                    if(registers_state[k] == 3) registers_state[k] = 0;
                    if(registers_state[k] == 4) registers_state[k] = 1;
                    if(registers_state[k] == 1) registers_state[k] = 0;
                }
                cycle++;
            }
            out2.close();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public static int jump(String inst, int pc) {
        String[] operands = inst.substring(inst.indexOf(' ')+1).split(", ");
        pc = Integer.valueOf(operands[0].substring(1));
        executed = "[" + inst + "]";
        return pc;
    }

    public static int beq(String inst, int pc) {
        String[] operands = inst.substring(inst.indexOf(' ')+1).split(", ");
        int r1 = 0, r2 = 0, ofs = 0;

        r1 = Integer.valueOf(operands[0].substring(1));
        r2 = Integer.valueOf(operands[1].substring(1));
        ofs = Integer.valueOf(operands[2].substring(1));
        if(branch_raw[r1] == 0 && branch_raw[r2] == 0 &&registers_state[r1]<2 && registers_state[r2]<2) {
            pc += 4;
            if(registers[r1] == registers[r2]) pc += ofs;
            executed = "[" + inst + "]";
            waiting = "";
            registers_state[r1] = 0;
            registers_state[r2] = 0;
        }
        else {
            waiting = "[" + inst + "]";
            executed = "";
        }
        return pc;
    }

    public static int bne(String inst, int pc) {
        String[] operands = inst.substring(inst.indexOf(' ')+1).split(", ");
        int r1 = 0, r2 = 0, ofs = 0;

        r1 = Integer.valueOf(operands[0].substring(1));
        r2 = Integer.valueOf(operands[1].substring(1));
        ofs = Integer.valueOf(operands[2].substring(1));
        if(branch_raw[r1] == 0 && branch_raw[r2] == 0 &&registers_state[r1]<2 && registers_state[r2]<2) {
            pc += 4;
            if(registers[r1] != registers[r2]) pc += ofs;
            executed = "[" + inst + "]";
            waiting = "";
            registers_state[r1] = 0;
            registers_state[r2] = 0;
        }
        else {
            waiting = "[" + inst + "]";
            executed = "";
        }
        return pc;
    }

    public static int bgtz(String inst, int pc) {
        String[] operands = inst.substring(inst.indexOf(' ')+1).split(", ");
        int r1 = 0, ofs = 0;
        r1 = Integer.valueOf(operands[0].substring(1));
        ofs = Integer.valueOf(operands[1].substring(1));
        if(branch_raw[r1] == 0  &&registers_state[r1]<2) {
            pc += 4;
            if(registers[r1] > 0) pc += ofs;
            executed = "[" + inst + "]";
            waiting = "";
            registers_state[r1] = 0;
        }
        else {
            waiting = "[" + inst + "]";
            executed ="";
        }
        return pc;
    }

    public static void output(BufferedWriter out2, int cycle) {
        try {
            String hyphens = "--------------------";
            out2.write(hyphens + "\r\n");
            out2.write("Cycle " + cycle + ":\r\n");
            out2.write("\r\nIF:\r\n");
            out2.write("\tWaiting: " + waiting + "\r\n");
            out2.write("\tExecuted: " + executed + "\r\n");

            out2.write("Buf1:\r\n");
            for(int m=0; m<buf1; m++) {
                out2.write("\tEntry " + m + ": ");
                if(m < buf[0].size()) out2.write("[" + buf[0].get(m) + "]" + "\r\n");
                else out2.write("\r\n");
            }

            out2.write("Buf2:\r\n");
            for(int m=0; m<buf2; m++) {
                out2.write("\tEntry " + m + ": ");
                if(m < buf[1].size()) out2.write("[" + buf[1].get(m) + "]" + "\r\n");
                else out2.write("\r\n");
            }

            out2.write("Buf3:\r\n");
            for(int m=0; m<buf3; m++) {
                out2.write("\tEntry " + m + ": ");
                if(m < buf[2].size()) out2.write("[" + buf[2].get(m) + "]" + "\r\n");
                else out2.write("\r\n");
            }

            out2.write("Buf4:\r\n");
            for(int m=0; m<buf4; m++) {
                out2.write("\tEntry " + m + ": ");
                if(m < buf[3].size()) out2.write("[" + buf[3].get(m) + "]" + "\r\n");
                else out2.write("\r\n");
            }

            out2.write("Buf5: ");
            if(buf[4].size() > 0) out2.write("[" + buf[4].get(0) + "]\r\n");
            else out2.write("\r\n");

            out2.write("Buf6: ");
            if(buf[5].size() > 0) out2.write(buf[5].get(0).substring(0, buf[5].get(0).indexOf(']')+1) + "\r\n");
            else out2.write("\r\n");

            out2.write("Buf7: ");
            if(buf[6].size() > 0) out2.write("[" + buf[6].get(0) + "]\r\n");
            else out2.write("\r\n");

            out2.write("Buf8: ");
            if(buf[7].size() > 0) out2.write(buf[7].get(0).substring(0, buf[7].get(0).indexOf(']')+1) + "\r\n");
            else out2.write("\r\n");

            out2.write("Buf9: ");
            if(buf[8].size() > 0) out2.write("[" + buf[8].get(0) + "]\r\n");
            else out2.write("\r\n");

            out2.write("Buf10: ");
            if(buf[9].size() > 0) out2.write(buf[9].get(0).substring(0, buf[9].get(0).indexOf(']')+1) + "\r\n");
            else out2.write("\r\n");

            out2.write("\r\nRegisters\n");
            int rg = 0;
            while(rg < 32) {
                if(rg == 0) out2.write("R00:");
                else if(rg == 8) out2.write("\r\nR08:");
                else if(rg == 16) out2.write("\r\nR16:");
                else if(rg == 24) out2.write("\r\nR24:");
                out2.write("\t"+registers[rg]);
                rg++;
            }
            out2.write("\r\n");
            out2.write("\r\n");

            out2.write("Data");
            int ds = data_start;
            int ct = 0;
            while(data.containsKey(ds)) {
                if(ct == 0) out2.write("\r\n" + ds + ":");
                out2.write("\t" + data.get(ds));
                ct = (ct+1) % 8;
                ds += 4;
            }
            out2.write("\r\n");
            out2.flush();
        }
        catch (IOException e) {
            e.printStackTrace();
        }

    }
}
