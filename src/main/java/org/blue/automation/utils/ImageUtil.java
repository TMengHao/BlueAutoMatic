package org.blue.automation.utils;

import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

/**
 * name: MengHao Tian
 * date: 2022/4/27 23:26
 */
public class ImageUtil {

    /**
     * 模板匹配,获取图像相似度
     *
     * @param originMat   源图像
     * @param templateMat 模板图像
     * @return 相似度
     **/
    public double getSimile(Mat originMat, Mat templateMat) {
        Mat resultMat = new Mat();
        int resCols = originMat.cols() - templateMat.cols() + 1;
        int resRows = originMat.rows() - templateMat.rows() + 1;
        resultMat.create(resRows, resCols, CvType.CV_32FC1);
        Imgproc.matchTemplate(originMat, templateMat, resultMat, Imgproc.TM_CCOEFF_NORMED);

        //HighGui.imshow("源图像",originMat);
        //HighGui.imshow("模板图像",templateMat);
        //HighGui.waitKey(0);
        return Core.minMaxLoc(resultMat).maxVal;
    }

    /**
     * 截取图片
     *
     * @param imagePath 图片路径
     * @param x 截取图片的x坐标
     * @param y 截取图片的y坐标
     * @param width 截取图片的宽
     * @param height 截取图片的高
     * @return 指定宽高的opencv的Mat对象
     **/
    public Mat interceptImage(String imagePath, int x,int y,int width,int height) {
        return Imgcodecs.imread(imagePath).submat(new Rect(x,y,width,height));
    }

    private ImageUtil() {
    }

    private static class ImageUtilHolder {
        private static final ImageUtil INSTANCE = new ImageUtil();
    }

    public static ImageUtil getInstance() {
        return ImageUtilHolder.INSTANCE;
    }
}
