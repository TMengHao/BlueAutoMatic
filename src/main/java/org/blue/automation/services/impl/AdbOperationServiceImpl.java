package org.blue.automation.services.impl;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.blue.automation.entities.AdbDevice;
import org.blue.automation.entities.AdbProvider;
import org.blue.automation.entities.enums.PathEnum;
import org.blue.automation.services.OperationService;
import org.blue.automation.utils.CMDUtil;
import org.opencv.core.Point;

import java.awt.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

/**
 * name: MengHao Tian
 * date: 2022/4/25 11:37
 */
public class AdbOperationServiceImpl implements OperationService {
    private static final Logger log = LogManager.getLogger(AdbOperationServiceImpl.class);
    private static final CMDUtil CMD_UTIL = CMDUtil.getInstance();
    private AdbProvider adbProvider;
    private boolean connected = false;

    @Override
    public void click(Point clickPoint) {
        try {
            if(notConnect()) throw new RuntimeException("ADB连接失败");
        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        try {
            CMD_UTIL.executeCMDCommand(
                    getAdbShellInput().append("tap").append(" ")
                            .append(clickPoint.x).append(" ").append(clickPoint.y).toString()
            );
        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
        }
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
                //随机延时(50~300ms)
                TimeUnit.MILLISECONDS.sleep((long) (Math.random()*250+50));
            } catch (InterruptedException e) {
                log.error("ADB多次点击出现异常:",e);
            }
        }
    }

    @Override
    public void slide(Point startPoint, Point endPoint) {
        longSlide(startPoint, endPoint, 50);
    }

    @Override
    public void longSlide(Point startPoint, Point endPoint, long delayTime) {
        try {
            if(notConnect()) throw new RuntimeException("ADB连接失败");
        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        try {
            CMD_UTIL.executeCMDCommand(
                    getAdbShellInput().append("swipe").append(" ")
                            .append(startPoint.x).append(" ").append(startPoint.y).append(" ")
                            .append(endPoint.x).append(" ").append(endPoint.y).append(" ")
                            .append(delayTime).toString()
            );
        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public void captureAndSave(String computerFile) {
        try {
            if(notConnect()) throw new RuntimeException("ADB连接失败");
        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        screenCap(adbProvider.getPhoneFilePath());
        pull(adbProvider.getPhoneFilePath(), computerFile);
    }

    @Override
    public void setFilePath(String filePath) {
        adbProvider = new AdbProviderServiceImpl(filePath).getAdbProvider();
    }

    private void screenCap(String phoneFile) {
        try {
            CMD_UTIL.executeCMDCommand(
                    getAdbShell().append("screencap").append(" ")
                            .append("-p").append(" ")
                            .append(phoneFile).toString()
            );
        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void pull(String phoneFile, String computerFile) {
        try {
            CMD_UTIL.executeCMDCommand(
                    getAdb().append("pull").append(" ")
                            .append(phoneFile).append(" ")
                            .append(computerFile).toString()
            );
        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private boolean notConnect() throws IOException, InterruptedException {
        if (connected) return false;
        //双重验证连接成功(连接设备+获取设备列表并且有活跃设备)
        if (adbProvider != null && connectToDevice(adbProvider.getDeviceNumber())) {
            ArrayList<AdbDevice> allDevices = getAllDevices();
            if(allDevices.size() > 0 && allDevices.stream().anyMatch(adbDevice -> adbDevice.getState().equals(AdbDevice.State.DEVICE))){
                connected = true;
                return false;
            }
        }
        connected = false;
        return true;
    }

    private boolean connectToDevice(String ipAddress) throws IOException, InterruptedException {
        String output = CMD_UTIL.executeCMDCommand(
                PathEnum.BIN + "adb.exe" + " " + "connect" + " " +
                        ipAddress
        );
        return output.contains("connected");
    }

    private ArrayList<AdbDevice> getAllDevices() throws IOException, InterruptedException {
        String output = CMD_UTIL.executeCMDCommand(PathEnum.BIN+"adb.exe" + " " + "devices");
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
        log.debug("adb所有设备:{}",deviceArrayList);
        return deviceArrayList;
    }

    private StringBuilder getAdb() {
        return new StringBuilder().append(PathEnum.BIN).append("adb.exe").append(" ")
                .append("-s").append(" ")
                .append(adbProvider.getDeviceNumber()).append(" ");
    }

    private StringBuilder getAdbShell() {
        return getAdb().append("shell").append(" ");
    }

    private StringBuilder getAdbShellInput() {
        return getAdbShell().append("input").append(" ");
    }
}
