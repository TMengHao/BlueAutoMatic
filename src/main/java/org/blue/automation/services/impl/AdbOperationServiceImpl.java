package org.blue.automation.services.impl;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.blue.automation.entities.AdbDevice;
import org.blue.automation.entities.Situation;
import org.blue.automation.services.OperationService;
import org.blue.automation.utils.CMDUtil;
import org.blue.automation.utils.ImageUtil;
import org.opencv.core.Point;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

/**
 * name: MengHao Tian
 * date: 2022/4/25 11:37
 */
public class AdbOperationServiceImpl implements OperationService {
    private static final Logger log = LogManager.getLogger(AdbOperationServiceImpl.class);
    private static final CMDUtil CMD_UTIL = CMDUtil.getInstance();
    private String deviceNumber = "192.168.100.21:5555";
    private static boolean CONNECTED = false;

    @Override
    public void click(Point clickPoint) {
        if(!connect()) throw new NullPointerException("ADB连接失败");
        CMD_UTIL.executeCMDCommand(
                getAdbShellInput().append("tap").append(" ")
                        .append(clickPoint.x).append(" ").append(clickPoint.y).toString()
        );
    }

    @Override
    public void longClick(Point clickPoint, long delayTime) {
        longSlide(clickPoint, clickPoint, delayTime);
    }

    @Override
    public void multipleClicks(ArrayList<Point> points) {
        for (Point point : points) {
            click(point);
            try {
                //随机延时(100~400ms)
                TimeUnit.MILLISECONDS.sleep((long) (Math.random()*300+100));
            } catch (InterruptedException e) {
                log.error("ADB点击等待出现异常:",e);
            }
        }
    }

    @Override
    public void slide(Point startPoint, Point endPoint) {
        longSlide(startPoint, endPoint, 50);
    }

    @Override
    public void longSlide(Point startPoint, Point endPoint, long delayTime) {
        if(!connect()) throw new NullPointerException("ADB连接失败");
        CMD_UTIL.executeCMDCommand(
                getAdbShellInput().append("swipe").append(" ")
                        .append(startPoint.x).append(" ").append(startPoint.y).append(" ")
                        .append(endPoint.x).append(" ").append(endPoint.y).append(" ")
                        .append(delayTime).toString()
        );
    }

    @Override
    public void captureAndSave(String computerFile) {
        if(!connect()) throw new NullPointerException("ADB连接失败");
        screenCap("/sdcard/blue_main.png");
        pull("/sdcard/blue_main.png", computerFile);
    }

    private void screenCap(String phoneFile) {
        CMD_UTIL.executeCMDCommand(
                getAdbShell().append("screencap").append(" ")
                        .append("-p").append(" ")
                        .append(phoneFile).toString()
        );
    }

    private void pull(String phoneFile, String computerFile) {
        CMD_UTIL.executeCMDCommand(
                getAdb().append("pull").append(" ")
                        .append(phoneFile).append(" ")
                        .append(computerFile).toString()
        );
    }

    private boolean connect() {
        if (CONNECTED) return true;
        //双重验证连接成功(连接设备+获取设备列表并且有活跃设备)
        ArrayList<AdbDevice> allDevices = getAllDevices();
        if (connectToDevice(deviceNumber) && allDevices.size() > 0 && allDevices.stream().anyMatch(adbDevice -> adbDevice.getState().equals(AdbDevice.State.DEVICE))) {
            CONNECTED = true;
            return true;
        }
        CONNECTED = false;
        return false;
    }

    private boolean connectToDevice(String ipAddress) {
        String output = CMD_UTIL.executeCMDCommand(
                getAdb().append("connect").append(" ")
                        .append(ipAddress).toString()
        );
        return output.contains("connected");
    }

    private ArrayList<AdbDevice> getAllDevices() {
        String output = CMD_UTIL.executeCMDCommand(getAdb().append("devices").toString());
        //分割控制台输出语句
        String[] split = output.split("\n");
        //判断当前语句之后是否为设备列表
        boolean isShow = false;
        ArrayList<AdbDevice> deviceArrayList = new ArrayList<>();
        //遍历控制台的每一行输出
        for (String str : split) {
            //根据\t进行当前行的分割,把设备名称和设备状态分开
            String[] device = str.split("\t");
            //如果开始显示设备列表,则进行存储,如果分割后的数组大于1个,则成功获取设备
            if (isShow && device.length > 1) {
                //选择设备的状态
                switch (device[1]) {
                    case "device":
                        deviceArrayList.add(new AdbDevice(device[0], AdbDevice.State.DEVICE));
                        break;
                    case "offline":
                        deviceArrayList.add(new AdbDevice(device[0], AdbDevice.State.OFFLINE));
                        break;
                }
            }
            //如果当前行含有attached,则下一行开始显示设备列表
            if (str.contains("attached")) isShow = true;
        }
        return deviceArrayList;
    }

    /*
    public void execOut(String computerFile) {
        CMD_UTIL.executeCMDCommand(
                getAdb().append("exec-out").append(" ")
                        .append("screencap").append(" ")
                        .append("-p").append(" ").append(">").append(" ")
                        .append(computerFile).toString()
        );
    }
    */

    public String getDeviceNumber() {
        return deviceNumber;
    }

    public void setDeviceNumber(String deviceNumber) {
        this.deviceNumber = deviceNumber;
    }

    private StringBuilder getAdb() {
        return new StringBuilder().append("adb").append(" ");
    }

    private StringBuilder getAdbShell() {
        return getAdb().append("shell").append(" ");
    }

    private StringBuilder getAdbShellInput() {
        return getAdbShell().append("input").append(" ");
    }
}
