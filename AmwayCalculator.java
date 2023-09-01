package com.jackrain.nea.retail.service;

import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * @description: 实现加减乘数
 * @author SKX009589
 * @date 2023/8/31 15:05
 * @version 1.0
 */
@Slf4j
public class AmwayCalculator {

    // 前面累计计算值
    private BigDecimal preTotal;
    // 新计算值
    private BigDecimal newNum;
    // 最近系列操作值集合
    private List<BigDecimal> lastNumList = new ArrayList<>();

    // 最近系列操作集合
    private List<String> lastOptList = new ArrayList<>();
    // 最近系列总值
    private List<BigDecimal> lastTotalList = new ArrayList<>();

    // 当前操作符
    private String currentOperator;

    // undo/redo最近操作索引
    private int lastOptIndex = -1;

    // 默认精度2位小数
    private int scale = 2;

    // undo/redo有效索引最大值
    private int validIndexMax = -1;

    public BigDecimal getPreTotal() {
        return preTotal;
    }

    public void setPreTotal(BigDecimal preTotal) {
        this.preTotal = preTotal;
    }

    public BigDecimal getNewNum() {
        return newNum;
    }

    public void setNewNum(BigDecimal newNum) {
        // 未计算过,累计总值为第一个输入值
        if (preTotal == null) {
            preTotal = newNum;
        } else {
            this.newNum = newNum;
        }
    }


    public void setCurrentOperator(String currentOperator) {
        this.currentOperator = currentOperator;
    }

    public void setScale(int scale) {
        this.scale = scale;
    }

    public static void main(String[] args) {
        AmwayCalculator calculator = new AmwayCalculator();

        calculator.setNewNum(new BigDecimal(3));
        calculator.setCurrentOperator("+");

        calculator.setNewNum(new BigDecimal(5));
        calculator.printResult();

        calculator.calc();
        calculator.printResult();

    }

    /**
     * @description: 计算方法入口
     * @author SKX009589
     * @date: 2023/8/31 15:35
     */
    public void calc() {
        preTotal = preTotal == null ? BigDecimal.ZERO : preTotal;
        if (currentOperator == null) {
            System.out.println("请选择操作!");
        }
        if (newNum != null) {
            // 进行计算
            BigDecimal result = calcNumber(preTotal, currentOperator, newNum);
            if (this.lastOptIndex == -1) {
                // 未处于redo/undo中间过程
                lastTotalList.add(preTotal);
                lastNumList.add(newNum);
                lastOptList.add(currentOperator);
            } else {
                // 处于redo/undo中间过程,覆盖undo/redo操作记录,并记录有效索引最大值
                this.lastOptIndex++;
                this.validIndexMax = this.lastOptIndex;
                this.lastTotalList.set(this.lastOptIndex, result);
                this.lastNumList.set(this.lastOptIndex - 1, newNum);
                this.lastOptList.set(this.lastOptIndex - 1, currentOperator);
            }
            preTotal = result;
            currentOperator = null;
            newNum = null;
        }
    }

    /**
     * @description: 回撤到上一步
     * @author SKX009589
     * @date 2023/8/31 18:12
     * @version 1.0
     */
    public void undo() {
        if (preTotal != null && lastOptIndex == -1) { // 未进行undo/redo操作,存储最后计算结果
            lastTotalList.add(preTotal);
            currentOperator = null;
            newNum = null;
        }

        if (lastTotalList.size() == 0) {
            System.out.println("无操作!");
        } else if (lastTotalList.size() == 1) {
            System.out.println("undo后值:0," + "undo前值:" + preTotal);
            preTotal = BigDecimal.ZERO;
        } else {
            if (lastOptIndex == -1) {
                lastOptIndex = lastOptList.size() - 1;
            } else {
                if (lastOptIndex - 1 < 0) {
                    System.out.println("无法再undo!");
                    return;
                }
                lastOptIndex--;
            }
            System.out.println("undo后值:" + lastTotalList.get(lastOptIndex) + ",undo前值:" + preTotal + ",undo的操作:" + lastOptList.get(lastOptIndex - 1) + ",undo操作的值:" + lastNumList.get(lastOptIndex - 1));
        }
    }


    public void redo() {
        try {
            if (lastOptIndex > -1) {
                if (lastOptIndex + 1 == lastTotalList.size() || lastOptIndex + 1 == this.validIndexMax + 1) {
                    System.out.println("无法再redo!");
                    return;
                }
                lastOptIndex++;
                System.out.println("redo后值:" + lastTotalList.get(lastOptIndex) + ",redo前值:" + preTotal + ",redo的操作:" + lastOptList.get(lastOptIndex - 1) + ",redo操作的值:" + lastNumList.get(lastOptIndex - 1));
            }
        } catch (Exception e) {
            System.out.println("redo异常,lastOptIndex:" + lastOptIndex);
        }
    }

    private void redoOperate(BigDecimal redoTotal, String redoOpt, BigDecimal redoNum) {
        System.out.println("redo后值:" + redoTotal + ",redo前值:" + preTotal + ",redo的操作:" + redoOpt + ",redo操作的值:" + redoNum);
        preTotal = redoTotal;
        currentOperator = null;
        newNum = null;
    }

    private void cancelPreOperate(BigDecimal lastTotal, String lastOpt, BigDecimal lastNum) {
        System.out.println("undo后值:" + lastTotal + ",undo前值:" + preTotal + ",undo的操作:" + lastOpt + ",undo操作的值:" + lastNum);
        preTotal = lastTotal;
        currentOperator = null;
        newNum = null;
    }


    /**
     * @description: 加减乘除操作
     * @author SKX009589
     * @date 2023/8/31 16:44
     * @version 1.0
     */
    private BigDecimal calcNumber(BigDecimal preTotal, String currentOperator, BigDecimal newNum) {
        BigDecimal result = BigDecimal.ZERO;
        // 为空时，默认做加法
        currentOperator = currentOperator == null ? "+" : currentOperator;
        switch (currentOperator) {
            case "+":
                result = preTotal.add(newNum);
                break;
            case "-":
                result = preTotal.subtract(newNum).setScale(scale, RoundingMode.HALF_UP);
                break;
            case "*":
                result = preTotal.multiply(newNum).setScale(scale, RoundingMode.HALF_UP);
                break;
            case "/":
                result = preTotal.divide(newNum, RoundingMode.HALF_UP);
                break;
        }
        return result;
    }

    // 打印操作结果
    public String printResult() {
        StringBuilder sb = new StringBuilder();
        if (preTotal != null) {
            sb.append(preTotal.setScale(scale, BigDecimal.ROUND_HALF_DOWN).toString());
        }
        if (currentOperator != null) {
            sb.append(currentOperator);
        }
        if (newNum != null) {
            sb.append(newNum);
        }
        System.out.println(sb.toString());
        return sb.toString();
    }

}