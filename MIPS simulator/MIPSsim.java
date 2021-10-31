/*On my honor, I have neither given nor received unauthorized aid on this assignment*/
import java.io.*;
import java.util.HashMap;

public class MIPSsim {
    static String input, output_disassembly, output_simulation;
    static HashMap<String, String> category[];
    static int[] registers;
    static HashMap<Integer, Integer> data;
    static HashMap<Integer, String> instructions;
    static int start;
    static int data_start;

    public static void main(String args[]) {

        input = new String(args[0]);
        //input = "/Users/user/desktop/uf/2021fall/cap/project1/data/sample.txt";
        //input = "/Users/user/desktop/uf/2021fall/cap/project1/data/test.txt";
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
                        //System.out.println("start="+start);
                        String next_address = Integer.toBinaryString(pos);
                        StringBuilder supple = new StringBuilder();
                        while(supple.length() < 32 - next_address.length()) supple.append("0") ;
                        next_address = supple + next_address;
                        //System.out.println(next_address);
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
        int adrs = start;
        int cycle = 1;
        String cyphens = "--------------------";
        try {
            File writename2 = new File(output_simulation);
            writename2.createNewFile();
            BufferedWriter out2 = new BufferedWriter(new FileWriter(writename2));
            while(!instructions.get(adrs).equals("BREAK")) {
                String inst = instructions.get(adrs);
                String operation = inst.substring(0, inst.indexOf(' '));
                String[] operands = inst.substring(inst.indexOf(' ')+1).split(", ");  //can add R and #

                int r1 = 0, r2 = 0, ofs = 0, r3 = 0, imdv = 0;

                out2.write(cyphens + "\r\n");
                out2.write("Cycle " + cycle + ":\t" + adrs + "\t" + inst + "\r\n");
                out2.write("\r\n");
                cycle++;

                switch(operation) {
                    case "J":
                        adrs = Integer.valueOf(operands[0].substring(1));
                        break;
                    case "BEQ":
                        r1 = Integer.valueOf(operands[0].substring(1));
                        r2 = Integer.valueOf(operands[1].substring(1));
                        ofs = Integer.valueOf(operands[2].substring(1));
                        adrs += 4;
                        if(registers[r1] == registers[r2]) adrs += ofs;
                        break;
                    case "BNE":
                        r1 = Integer.valueOf(operands[0].substring(1));
                        r2 = Integer.valueOf(operands[1].substring(1));
                        ofs = Integer.valueOf(operands[2].substring(1));
                        adrs += 4;
                        if(registers[r1] != registers[r2]) adrs += ofs;
                        break;
                    case "BGTZ": //if registers[r1]>0, then branck
                        r1 = Integer.valueOf(operands[0].substring(1));
                        ofs = Integer.valueOf(operands[1].substring(1));
                        adrs += 4;
                        if(registers[r1] > 0) adrs += ofs;
                        break;
                    case "SW":  //SW R5, 348(R6)
                        r1 = Integer.valueOf(operands[0].substring(1));
                        ofs = Integer.valueOf(operands[1].substring(0, operands[1].indexOf("(")));
                        r2 = Integer.valueOf(operands[1].substring(operands[1].indexOf("(")+2,operands[1].indexOf(")")));
                        data.put(ofs+registers[r2], registers[r1]);
                        adrs += 4;
                        break;
                    case "LW":
                        r1 = Integer.valueOf(operands[0].substring(1));
                        ofs = Integer.valueOf(operands[1].substring(0, operands[1].indexOf("(")));
                        r2 = Integer.valueOf(operands[1].substring(operands[1].indexOf("(")+2,operands[1].indexOf(")")));
                        registers[r1] = data.get(registers[r2] + ofs);
                        adrs += 4;
                        break;
                    case "ADD":
                        r1 = Integer.valueOf(operands[0].substring(1));
                        r2 = Integer.valueOf(operands[1].substring(1));
                        r3 = Integer.valueOf(operands[2].substring(1));
                        registers[r1] = registers[r2] + registers[r3];
                        adrs += 4;
                        break;
                    case "SUB":
                        r1 = Integer.valueOf(operands[0].substring(1));
                        r2 = Integer.valueOf(operands[1].substring(1));
                        r3 = Integer.valueOf(operands[2].substring(1));
                        registers[r1] = registers[r2] - registers[r3];
                        adrs += 4;
                        break;
                    case "AND":
                        r1 = Integer.valueOf(operands[0].substring(1));
                        r2 = Integer.valueOf(operands[1].substring(1));
                        r3 = Integer.valueOf(operands[2].substring(1));
                        registers[r1] = registers[r2] & registers[r3];
                        adrs += 4;
                        break;
                    case "OR":
                        r1 = Integer.valueOf(operands[0].substring(1));
                        r2 = Integer.valueOf(operands[1].substring(1));
                        r3 = Integer.valueOf(operands[2].substring(1));
                        registers[r1] = registers[r2] | registers[r3];
                        adrs += 4;
                        break;
                    case "SRL":  //r1 <- r2 >>> imdv, unsigned
                        r1 = Integer.valueOf(operands[0].substring(1));
                        r2 = Integer.valueOf(operands[1].substring(1));
                        imdv = Integer.valueOf(operands[2].substring(1));
                        registers[r1] = registers[r2] >>> imdv;
                        adrs += 4;
                        break;
                    case "SRA":
                        r1 = Integer.valueOf(operands[0].substring(1));
                        r2 = Integer.valueOf(operands[1].substring(1));
                        imdv = Integer.valueOf(operands[2].substring(1));
                        registers[r1] = registers[r2] >> imdv;
                        adrs += 4;
                        break;
                    case "MUL":
                        r1 = Integer.valueOf(operands[0].substring(1));
                        r2 = Integer.valueOf(operands[1].substring(1));
                        r3 = Integer.valueOf(operands[2].substring(1));
                        Long tp = (long)registers[r2] * (long)registers[r3];
                        String tmp = Long.toBinaryString(tp);
                        if(tmp.length()>32) registers[r1] = Integer.parseUnsignedInt(tmp.substring(tmp.length()-32),2);
                        else registers[r1] = Integer.parseUnsignedInt(tmp,2);
                        adrs += 4;
                        break;
                    case "ADDI":
                        r1 = Integer.valueOf(operands[0].substring(1));
                        r2 = Integer.valueOf(operands[1].substring(1));
                        imdv = Integer.valueOf(operands[2].substring(1));
                        registers[r1] = registers[r2] + imdv;
                        adrs += 4;
                        break;
                    case "ANDI":
                        r1 = Integer.valueOf(operands[0].substring(1));
                        r2 = Integer.valueOf(operands[1].substring(1));
                        imdv = Integer.valueOf(operands[2].substring(1));
                        registers[r1] = registers[r2] & imdv;
                        adrs += 4;
                        break;
                    case "ORI":
                        r1 = Integer.valueOf(operands[0].substring(1));
                        r2 = Integer.valueOf(operands[1].substring(1));
                        imdv = Integer.valueOf(operands[2].substring(1));
                        registers[r1] = registers[r2] | imdv;
                        adrs += 4;
                        break;
                }

                out2.write("Registers\r\n");
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
                out2.write("\r\n");
            }

            //ADD BREAK CIRCLE
            out2.write(cyphens + "\r\n");
            out2.write("Cycle" + cycle + ":\t" + adrs + "\tBREAK\r\n");
            out2.write("\r\n");

            out2.write("Registers\r\n");
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
            out2.write("\r\n");


            out2.flush();
            out2.close();
        }
        catch(Exception e) {
            e.printStackTrace();
        }
    }
}
