1. This file uses Java to create a simple MIPS simulator which will perform two tasks:
1) Load a specified MIPS text file (sample.txt) and generate the assembly code equivalent to the input file by disassembler();
2) Genrate the instruction-by-instruction simulation of the MIPS code by simulator(). It should also produce the contents of registers and data memories after execution of each instruction. 



2. Before continuing:
1) Each instruction is 32 bits, and it is comosed of three parts: 
	3-bit category code | 3-bit opcode | operands
2) If the format not decribed clearly in this file, please refer to MIPS.pdf, which is MIPS instruction set manual.
3) Exception or interrupt handlings are not implemented in this project.



3. This file supports three categories of instructions. Supported instructions and format of instructions of each type are described as follows:
1) Category-1
----Support instructions: J, BEQ, BNE, BGTZ, SW, LW, BREAK
----Format of instructions: 
	000 | Opcode (3 bits) | Same as MIPS instruction
----Opcode for each instruction:
	Instruction	|	Opcode
	J		|	000
	BEQ		|	001
	BNE		|	010
	BGTZ		|	011
	SW		|	100
	LW		|	101
	BREAK		|	110
----Pay attention to the exact description of instruction formats and its interpretation in MIPS instruction set manual. For example, in case of J instruction, the 26-bit instruction_index is shifted left by 2 bits (padded with 00 at LSB side) and then the leftmost (MSB side) 4 bits of the delay slot instruction are used to form the 4 bits (MSB side) of the target address. Since we do not use delay slot in this project, treat the address of the next intruction as the address of the delay slot instructio. Similarly, for BEQ, BNE and BGTZ instructions, the 16-bit offset is shifted left by 2 bits to form 18-bit signed offset that is added with the address of the next instruction to form the target address. Note that we do not consider delay slot for this project. In other words, an instruction following the branch instruction should be treated as a regular instruction.
----Once you look at the sample_disassembly.txt in the project assignment, it may be confusing for you to see that the last 16 bits of the following binary has the value of 9 but the assembly shows it as 36. This is a convention issure with MIPS. The binary always shows the actual offset (9 in this case) value. Howver, the assembly always shows the value shifted by 2 bits to the left.
	0000010000100010 0000000000001001	276 BEQ R1, R2, #36
2) Category-2
----Support instructions: ADD, SUB, AND, OR, SRL, SRA, MUL
----Format of instructions: dest <- src1 op src2
	001 | Opcode (3 bits) | dest (5 bits) | src1 (5 bits) | src2 (5 bits) | 00000000000
----Opcode for each instruction:
	Instruction	|	Opcode
	ADD		|	000
	SUB		|	001
	AND		|	010
	OR		|	011
	SRL		|	100
	SRA		|	101
	MUL		|	110
----The src1 is always register but src2 can be register or immediate value depending on the opcode. For ADD, SUB, AND, OR and MUL,src2 is register. For SRL and SRA, src2 is immediate value.
3) Category-3
----Support instructions: ADDI, ANDI, ORI
----Format of instructions: dest <- src 1 op immediate_value
	010 | opcode (3 bits) | dest (5 bits) | src1 (5 bits) | immediate_value (16 bits)
----Opcode for each instruction:
	Instruction	|	Opcode
	ADDI		|	000
	ANDI		|	001
	ORI		|	010



4. Sample input/output files:
1) Input file: inputfilename.txt (eg: sample,txt). 
----This file contains a sequence of 32-bit instruction words starting at address "260". The final isntruction in the sequence of instructions is always BREAK. There will be only one BREAK instruction. Following the BREAK instruction (immediately after BREAK), there is a sequence of 32-bit 2's complement signed integers for the program data up to the end of the file. 
----The newline character can be either “\n” (linux) or “\r\n” (windows).  
2) Output files: disassembly.txt and simulator.txt.
----Your MIPS simulator (with executable name as MIPSsim) should produce two output files in the same directory: disassembly.txt (contains disassembled output) and simulation.txt (contains the simulation trace). 
----The disassembler output file should contain 3 columns of data with each column separated by one tab character (‘\t’ or char(9)).
----Note, if you are displaying an instruction, the third column should contain every part of the instruction, with each argument separated by a comma and then a space (“, ”).
----The simulation output file should have the following format:
	20 hyphens and a new line
	Cycle < cycleNumber >:< tab >< instr_Address >< tab >< instr_string >
	< blank_line >
	Registers
	R00: < tab >< int(R0) >< tab >< int(R1) >...< tab >< int(R7) >
	R08: < tab >< int(R8) >< tab >< int(R9) >...< tab >< int(R15) >
	R16: < tab >< int(R16) >< tab >< int(R17) >...< tab >< int(R23) >
	R24: < tab >< int(R24) >< tab >< int(R25) >...< tab >< int(R31) >
	< blank_line >
	Data
	< firstDataAddress >: < tab >< display 8 data words as integers with tabs in between > ..... < 		continue until the last data word >
----The instructions and instruction arguments should be in capital letters. Display all integer values in decimal. Immediate values should be preceded by a “#” symbol. Note that some instructions take signed immediate values while others take unsigned immediate values.

5. Test steps:
1) javac MIPSsim.java
2) java MIPSsim inputfilename.txt
3) diff -w -B disassembly.txt sample_disassembly.txt
4) diff –w –B simulation.txt sample_simulation.txt
