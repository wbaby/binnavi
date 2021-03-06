/*
Copyright 2014 Google Inc. All Rights Reserved.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/
package com.google.security.zynamics.reil.translators.x86;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.google.common.collect.Lists;
import com.google.security.zynamics.reil.OperandSize;
import com.google.security.zynamics.reil.ReilInstruction;
import com.google.security.zynamics.reil.TestHelpers;
import com.google.security.zynamics.reil.interpreter.CpuPolicyX86;
import com.google.security.zynamics.reil.interpreter.EmptyInterpreterPolicy;
import com.google.security.zynamics.reil.interpreter.Endianness;
import com.google.security.zynamics.reil.interpreter.InterpreterException;
import com.google.security.zynamics.reil.interpreter.ReilInterpreter;
import com.google.security.zynamics.reil.interpreter.ReilRegisterStatus;
import com.google.security.zynamics.reil.translators.InternalTranslationException;
import com.google.security.zynamics.reil.translators.StandardEnvironment;
import com.google.security.zynamics.reil.translators.x86.SarTranslator;
import com.google.security.zynamics.zylib.disassembly.ExpressionType;
import com.google.security.zynamics.zylib.disassembly.IInstruction;
import com.google.security.zynamics.zylib.disassembly.MockInstruction;
import com.google.security.zynamics.zylib.disassembly.MockOperandTree;
import com.google.security.zynamics.zylib.disassembly.MockOperandTreeNode;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.math.BigInteger;
import java.util.ArrayList;

@RunWith(JUnit4.class)
public class SarTranslatorTest {
  private final ReilInterpreter interpreter = new ReilInterpreter(Endianness.LITTLE_ENDIAN,
      new CpuPolicyX86(), new EmptyInterpreterPolicy());
  private final StandardEnvironment environment = new StandardEnvironment();
  private final SarTranslator translator = new SarTranslator();
  private final ArrayList<ReilInstruction> instructions = new ArrayList<ReilInstruction>();

  @Test
  public void testDWordBoundaryShift() throws InternalTranslationException, InterpreterException {
    interpreter.setRegister("AF", BigInteger.ZERO, OperandSize.DWORD, ReilRegisterStatus.UNDEFINED);
    interpreter.setRegister("CF", BigInteger.ZERO, OperandSize.DWORD, ReilRegisterStatus.UNDEFINED);
    interpreter.setRegister("OF", BigInteger.ZERO, OperandSize.DWORD, ReilRegisterStatus.UNDEFINED);
    interpreter.setRegister("SF", BigInteger.ZERO, OperandSize.DWORD, ReilRegisterStatus.UNDEFINED);
    interpreter.setRegister("ZF", BigInteger.ZERO, OperandSize.DWORD, ReilRegisterStatus.UNDEFINED);
    interpreter.setRegister("eax", BigInteger.valueOf(0x7FFFFFFF), OperandSize.DWORD,
        ReilRegisterStatus.DEFINED);

    final MockOperandTree operandTree1 = new MockOperandTree();
    operandTree1.root = new MockOperandTreeNode(ExpressionType.SIZE_PREFIX, "dword");
    operandTree1.root.m_children.add(new MockOperandTreeNode(ExpressionType.REGISTER, "eax"));

    final MockOperandTree operandTree2 = new MockOperandTree();
    operandTree2.root = new MockOperandTreeNode(ExpressionType.SIZE_PREFIX, "dword");
    operandTree2.root.m_children
        .add(new MockOperandTreeNode(ExpressionType.IMMEDIATE_INTEGER, "3"));
    final IInstruction instruction =
        new MockInstruction("sar", Lists.newArrayList(operandTree1, operandTree2));

    translator.translate(environment, instruction, instructions);
    interpreter.interpret(TestHelpers.createMapping(instructions), BigInteger.valueOf(0x100));

    assertEquals(5, TestHelpers.filterNativeRegisters(interpreter.getDefinedRegisters()).size());
    assertEquals(BigInteger.valueOf(0xFFFFFFF), interpreter.getVariableValue("eax"));
    assertEquals(BigInteger.ONE, interpreter.getVariableValue("CF"));
    assertEquals(BigInteger.ZERO, interpreter.getVariableValue("ZF"));
    assertEquals(BigInteger.ZERO, interpreter.getVariableValue("SF"));
    assertFalse(interpreter.isDefined("AF"));
    assertFalse(interpreter.isDefined("OF"));
    assertTrue(interpreter.isDefined("CF"));
    assertTrue(interpreter.isDefined("SF"));
    assertTrue(interpreter.isDefined("ZF"));

    assertEquals(BigInteger.ZERO, BigInteger.valueOf(interpreter.getMemorySize()));
  }

  @Test
  public void testDWordShiftNegative() throws InternalTranslationException, InterpreterException {
    interpreter.setRegister("AF", BigInteger.ZERO, OperandSize.DWORD, ReilRegisterStatus.UNDEFINED);
    interpreter.setRegister("CF", BigInteger.ZERO, OperandSize.DWORD, ReilRegisterStatus.UNDEFINED);
    interpreter.setRegister("OF", BigInteger.ZERO, OperandSize.DWORD, ReilRegisterStatus.UNDEFINED);
    interpreter.setRegister("SF", BigInteger.ZERO, OperandSize.DWORD, ReilRegisterStatus.UNDEFINED);
    interpreter.setRegister("ZF", BigInteger.ZERO, OperandSize.DWORD, ReilRegisterStatus.UNDEFINED);
    interpreter.setRegister("eax", BigInteger.valueOf(0xFFFFFFFF), OperandSize.DWORD,
        ReilRegisterStatus.DEFINED);

    final MockOperandTree operandTree1 = new MockOperandTree();
    operandTree1.root = new MockOperandTreeNode(ExpressionType.SIZE_PREFIX, "dword");
    operandTree1.root.m_children.add(new MockOperandTreeNode(ExpressionType.REGISTER, "eax"));

    final MockOperandTree operandTree2 = new MockOperandTree();
    operandTree2.root = new MockOperandTreeNode(ExpressionType.SIZE_PREFIX, "dword");
    operandTree2.root.m_children
        .add(new MockOperandTreeNode(ExpressionType.IMMEDIATE_INTEGER, "3"));
    final IInstruction instruction =
        new MockInstruction("sar", Lists.newArrayList(operandTree1, operandTree2));

    translator.translate(environment, instruction, instructions);
    interpreter.interpret(TestHelpers.createMapping(instructions), BigInteger.valueOf(0x100));

    assertEquals(5, TestHelpers.filterNativeRegisters(interpreter.getDefinedRegisters()).size());
    assertEquals(new BigInteger("FFFFFFFF", 16), interpreter.getVariableValue("eax"));
    assertEquals(BigInteger.ONE, interpreter.getVariableValue("CF"));
    assertEquals(BigInteger.ZERO, interpreter.getVariableValue("ZF"));
    assertEquals(BigInteger.ONE, interpreter.getVariableValue("SF"));
    assertFalse(interpreter.isDefined("AF"));
    assertFalse(interpreter.isDefined("OF"));
    assertTrue(interpreter.isDefined("CF"));
    assertTrue(interpreter.isDefined("SF"));
    assertTrue(interpreter.isDefined("ZF"));

    assertEquals(BigInteger.ZERO, BigInteger.valueOf(interpreter.getMemorySize()));
  }

  @Test
  public void testDWordShiftPositive() throws InternalTranslationException, InterpreterException {
    interpreter.setRegister("AF", BigInteger.ZERO, OperandSize.DWORD, ReilRegisterStatus.UNDEFINED);
    interpreter.setRegister("CF", BigInteger.ZERO, OperandSize.DWORD, ReilRegisterStatus.UNDEFINED);
    interpreter.setRegister("OF", BigInteger.ZERO, OperandSize.DWORD, ReilRegisterStatus.UNDEFINED);
    interpreter.setRegister("PF", BigInteger.ZERO, OperandSize.DWORD, ReilRegisterStatus.UNDEFINED);
    interpreter.setRegister("SF", BigInteger.ZERO, OperandSize.DWORD, ReilRegisterStatus.UNDEFINED);
    interpreter.setRegister("ZF", BigInteger.ZERO, OperandSize.DWORD, ReilRegisterStatus.UNDEFINED);
    interpreter.setRegister("eax", BigInteger.valueOf(21), OperandSize.DWORD,
        ReilRegisterStatus.DEFINED);

    final MockOperandTree operandTree1 = new MockOperandTree();
    operandTree1.root = new MockOperandTreeNode(ExpressionType.SIZE_PREFIX, "dword");
    operandTree1.root.m_children.add(new MockOperandTreeNode(ExpressionType.REGISTER, "eax"));

    final MockOperandTree operandTree2 = new MockOperandTree();
    operandTree2.root = new MockOperandTreeNode(ExpressionType.SIZE_PREFIX, "dword");
    operandTree2.root.m_children
        .add(new MockOperandTreeNode(ExpressionType.IMMEDIATE_INTEGER, "1"));
    final IInstruction instruction =
        new MockInstruction("sar", Lists.newArrayList(operandTree1, operandTree2));

    translator.translate(environment, instruction, instructions);
    interpreter.interpret(TestHelpers.createMapping(instructions), BigInteger.valueOf(0x100));

    assertEquals(6, TestHelpers.filterNativeRegisters(interpreter.getDefinedRegisters()).size());
    assertEquals(BigInteger.valueOf(10), interpreter.getVariableValue("eax"));
    assertEquals(BigInteger.ONE, interpreter.getVariableValue("CF"));
    assertEquals(BigInteger.ZERO, interpreter.getVariableValue("ZF"));
    assertEquals(BigInteger.ZERO, interpreter.getVariableValue("SF"));
    assertFalse(interpreter.isDefined("AF"));
    assertTrue(interpreter.isDefined("OF"));
    assertTrue(interpreter.isDefined("CF"));
    assertTrue(interpreter.isDefined("SF"));
    assertTrue(interpreter.isDefined("ZF"));

    assertEquals(BigInteger.ZERO, BigInteger.valueOf(interpreter.getMemorySize()));
  }

  @Test
  // Test -9/4
  public void testSignedShift() throws InternalTranslationException, InterpreterException {
    interpreter.setRegister("AF", BigInteger.ZERO, OperandSize.DWORD, ReilRegisterStatus.UNDEFINED);
    interpreter.setRegister("CF", BigInteger.ZERO, OperandSize.DWORD, ReilRegisterStatus.UNDEFINED);
    interpreter.setRegister("OF", BigInteger.ZERO, OperandSize.DWORD, ReilRegisterStatus.UNDEFINED);
    interpreter.setRegister("PF", BigInteger.ZERO, OperandSize.DWORD, ReilRegisterStatus.UNDEFINED);
    interpreter.setRegister("SF", BigInteger.ZERO, OperandSize.DWORD, ReilRegisterStatus.UNDEFINED);
    interpreter.setRegister("ZF", BigInteger.ZERO, OperandSize.DWORD, ReilRegisterStatus.UNDEFINED);
    interpreter.setRegister("eax", BigInteger.valueOf(0xFFFFFFF7), OperandSize.DWORD,
        ReilRegisterStatus.DEFINED);

    final MockOperandTree operandTree1 = new MockOperandTree();
    operandTree1.root = new MockOperandTreeNode(ExpressionType.SIZE_PREFIX, "dword");
    operandTree1.root.m_children.add(new MockOperandTreeNode(ExpressionType.REGISTER, "eax"));

    final MockOperandTree operandTree2 = new MockOperandTree();
    operandTree2.root = new MockOperandTreeNode(ExpressionType.SIZE_PREFIX, "dword");
    operandTree2.root.m_children
        .add(new MockOperandTreeNode(ExpressionType.IMMEDIATE_INTEGER, "2"));
    final IInstruction instruction =
        new MockInstruction("sar", Lists.newArrayList(operandTree1, operandTree2));

    translator.translate(environment, instruction, instructions);
    interpreter.interpret(TestHelpers.createMapping(instructions), BigInteger.valueOf(0x100));

    assertEquals(5, TestHelpers.filterNativeRegisters(interpreter.getDefinedRegisters()).size());
    assertEquals(new BigInteger("FFFFFFFD", 16), interpreter.getVariableValue("eax"));
    assertEquals(BigInteger.ONE, interpreter.getVariableValue("CF"));
    assertEquals(BigInteger.ZERO, interpreter.getVariableValue("ZF"));
    assertEquals(BigInteger.ONE, interpreter.getVariableValue("SF"));
    assertFalse(interpreter.isDefined("AF"));
    assertFalse(interpreter.isDefined("OF"));
    assertTrue(interpreter.isDefined("CF"));
    assertTrue(interpreter.isDefined("SF"));
    assertTrue(interpreter.isDefined("ZF"));

    assertEquals(BigInteger.ZERO, BigInteger.valueOf(interpreter.getMemorySize()));
  }

  @Test
  public void testSingleShift() throws InternalTranslationException, InterpreterException {
    interpreter.setRegister("AF", BigInteger.ZERO, OperandSize.DWORD, ReilRegisterStatus.UNDEFINED);
    interpreter.setRegister("CF", BigInteger.ZERO, OperandSize.DWORD, ReilRegisterStatus.UNDEFINED);
    interpreter.setRegister("OF", BigInteger.ZERO, OperandSize.DWORD, ReilRegisterStatus.UNDEFINED);
    interpreter.setRegister("PF", BigInteger.ZERO, OperandSize.DWORD, ReilRegisterStatus.UNDEFINED);
    interpreter.setRegister("SF", BigInteger.ZERO, OperandSize.DWORD, ReilRegisterStatus.UNDEFINED);
    interpreter.setRegister("ZF", BigInteger.ZERO, OperandSize.DWORD, ReilRegisterStatus.UNDEFINED);
    interpreter.setRegister("eax", BigInteger.valueOf(0x11), OperandSize.DWORD,
        ReilRegisterStatus.DEFINED);

    final MockOperandTree operandTree1 = new MockOperandTree();
    operandTree1.root = new MockOperandTreeNode(ExpressionType.SIZE_PREFIX, "dword");
    operandTree1.root.m_children.add(new MockOperandTreeNode(ExpressionType.REGISTER, "eax"));

    final MockOperandTree operandTree2 = new MockOperandTree();
    operandTree2.root = new MockOperandTreeNode(ExpressionType.SIZE_PREFIX, "dword");
    operandTree2.root.m_children
        .add(new MockOperandTreeNode(ExpressionType.IMMEDIATE_INTEGER, "1"));
    final IInstruction instruction =
        new MockInstruction("sar", Lists.newArrayList(operandTree1, operandTree2));

    translator.translate(environment, instruction, instructions);
    interpreter.interpret(TestHelpers.createMapping(instructions), BigInteger.valueOf(0x100));

    assertEquals(6, TestHelpers.filterNativeRegisters(interpreter.getDefinedRegisters()).size());
    assertEquals(BigInteger.valueOf(8), interpreter.getVariableValue("eax"));
    assertEquals(BigInteger.ONE, interpreter.getVariableValue("CF"));
    assertEquals(BigInteger.ZERO, interpreter.getVariableValue("OF"));
    assertEquals(BigInteger.ZERO, interpreter.getVariableValue("ZF"));
    assertEquals(BigInteger.ZERO, interpreter.getVariableValue("SF"));
    assertFalse(interpreter.isDefined("AF"));
    assertTrue(interpreter.isDefined("OF"));
    assertTrue(interpreter.isDefined("CF"));
    assertTrue(interpreter.isDefined("SF"));
    assertTrue(interpreter.isDefined("ZF"));

    assertEquals(BigInteger.ZERO, BigInteger.valueOf(interpreter.getMemorySize()));
  }

  @Test
  public void testWordShift() throws InternalTranslationException, InterpreterException {
    interpreter.setRegister("AF", BigInteger.ZERO, OperandSize.DWORD, ReilRegisterStatus.UNDEFINED);
    interpreter.setRegister("CF", BigInteger.ZERO, OperandSize.DWORD, ReilRegisterStatus.UNDEFINED);
    interpreter.setRegister("OF", BigInteger.ZERO, OperandSize.DWORD, ReilRegisterStatus.UNDEFINED);
    interpreter.setRegister("PF", BigInteger.ZERO, OperandSize.DWORD, ReilRegisterStatus.UNDEFINED);
    interpreter.setRegister("SF", BigInteger.ZERO, OperandSize.DWORD, ReilRegisterStatus.UNDEFINED);
    interpreter.setRegister("ZF", BigInteger.ZERO, OperandSize.DWORD, ReilRegisterStatus.UNDEFINED);
    interpreter.setRegister("eax", BigInteger.valueOf(0xFFFF), OperandSize.DWORD,
        ReilRegisterStatus.DEFINED);

    final MockOperandTree operandTree1 = new MockOperandTree();
    operandTree1.root = new MockOperandTreeNode(ExpressionType.SIZE_PREFIX, "dword");
    operandTree1.root.m_children.add(new MockOperandTreeNode(ExpressionType.REGISTER, "eax"));

    final MockOperandTree operandTree2 = new MockOperandTree();
    operandTree2.root = new MockOperandTreeNode(ExpressionType.SIZE_PREFIX, "dword");
    operandTree2.root.m_children
        .add(new MockOperandTreeNode(ExpressionType.IMMEDIATE_INTEGER, "3"));
    final IInstruction instruction =
        new MockInstruction("sar", Lists.newArrayList(operandTree1, operandTree2));

    translator.translate(environment, instruction, instructions);
    interpreter.interpret(TestHelpers.createMapping(instructions), BigInteger.valueOf(0x100));

    System.out.println("registers: "
        + TestHelpers.filterNativeRegisters(interpreter.getDefinedRegisters()));
    assertEquals(5, TestHelpers.filterNativeRegisters(interpreter.getDefinedRegisters()).size());
    assertEquals(BigInteger.valueOf(0x1FFF), interpreter.getVariableValue("eax"));
    assertEquals(BigInteger.ONE, interpreter.getVariableValue("CF"));
    assertEquals(BigInteger.ZERO, interpreter.getVariableValue("ZF"));
    assertEquals(BigInteger.ZERO, interpreter.getVariableValue("SF"));
    assertFalse(interpreter.isDefined("AF"));
    assertFalse(interpreter.isDefined("OF"));
    assertTrue(interpreter.isDefined("CF"));
    assertTrue(interpreter.isDefined("SF"));
    assertTrue(interpreter.isDefined("ZF"));

    assertEquals(BigInteger.ZERO, BigInteger.valueOf(interpreter.getMemorySize()));
  }

  @Test
  public void testZeroShift() throws InternalTranslationException, InterpreterException {
    interpreter.setRegister("AF", BigInteger.ONE, OperandSize.DWORD, ReilRegisterStatus.UNDEFINED);
    interpreter.setRegister("CF", BigInteger.ONE, OperandSize.DWORD, ReilRegisterStatus.UNDEFINED);
    interpreter.setRegister("OF", BigInteger.ONE, OperandSize.DWORD, ReilRegisterStatus.UNDEFINED);
    interpreter.setRegister("SF", BigInteger.ONE, OperandSize.DWORD, ReilRegisterStatus.UNDEFINED);
    interpreter.setRegister("ZF", BigInteger.ONE, OperandSize.DWORD, ReilRegisterStatus.UNDEFINED);
    interpreter.setRegister("eax", BigInteger.valueOf(0x123), OperandSize.DWORD,
        ReilRegisterStatus.DEFINED);

    final MockOperandTree operandTree1 = new MockOperandTree();
    operandTree1.root = new MockOperandTreeNode(ExpressionType.SIZE_PREFIX, "dword");
    operandTree1.root.m_children.add(new MockOperandTreeNode(ExpressionType.REGISTER, "eax"));

    final MockOperandTree operandTree2 = new MockOperandTree();
    operandTree2.root = new MockOperandTreeNode(ExpressionType.SIZE_PREFIX, "dword");
    operandTree2.root.m_children
        .add(new MockOperandTreeNode(ExpressionType.IMMEDIATE_INTEGER, "0"));
    final IInstruction instruction =
        new MockInstruction("sar", Lists.newArrayList(operandTree1, operandTree2));

    translator.translate(environment, instruction, instructions);
    interpreter.interpret(TestHelpers.createMapping(instructions), BigInteger.valueOf(0x100));

    assertEquals(2, TestHelpers.filterNativeRegisters(interpreter.getDefinedRegisters()).size());
    assertEquals(BigInteger.valueOf(0x123), interpreter.getVariableValue("eax"));
    assertFalse(interpreter.isDefined("AF"));
    assertFalse(interpreter.isDefined("CF"));
    assertFalse(interpreter.isDefined("OF"));
    assertFalse(interpreter.isDefined("SF"));
    assertFalse(interpreter.isDefined("ZF"));

    assertEquals(BigInteger.ZERO, BigInteger.valueOf(interpreter.getMemorySize()));
  }
}
