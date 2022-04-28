package org.blue.automation.thread;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.blue.automation.Main;
import org.blue.automation.entities.Mode;
import org.blue.automation.entities.Situation;
import org.blue.automation.entities.SituationImage;
import org.blue.automation.entities.enums.Action;
import org.blue.automation.entities.enums.PathEnum;
import org.blue.automation.services.OperationModeService;
import org.blue.automation.services.impl.AdbOperationServiceImpl;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * name: MengHao Tian
 * date: 2022/4/25 15:41
 */
public class ModeCallable implements Callable<Boolean> {
    private final static Logger log = LogManager.getLogger(ModeCallable.class);
    private final ExecutorService THREAD_POOL = Main.THREAD_POOL;
    private final OperationModeService operationModeService;
    private final Mode mode;

    public ModeCallable(Mode mode, OperationModeService operationModeService) {
        this.mode = mode;
        this.operationModeService = operationModeService;
    }

    @Override
    public Boolean call() {
        ArrayList<Situation> situationArrayList = mode.getSituations();
        //如果没有情景列表,则直接返回false
        if (situationArrayList == null || situationArrayList.size() <= 0) return false;
        //如果adb连接失败,则直接退出
        if (!operationModeService.isConnected(operationModeService.getAllDevices().get(0))) return false;
        log.info("adb连接成功,开始运行:{}", mode);
        //创建接收完成任务的线程池
        CompletionService<Situation> completionService = new ExecutorCompletionService<>(THREAD_POOL);
        //接收任务的列表
        ArrayList<Future<Situation>> futureArrayList = new ArrayList<>();
        //初始时间
        long initMillis = System.currentTimeMillis(), waitTime = 0;
        //判断是否匹配成功,匹配成功则更新初始时间,失败则不更新初始时间
        boolean isMatch = true;
        //最终对象
        Situation endSituation = new Situation();
        while (!Thread.currentThread().isInterrupted() && waitTime < 15000) {
            try {
                futureArrayList.clear();
                clearSituation(endSituation);
                if (isMatch) initMillis = System.currentTimeMillis();
                operationModeService.captureAndSave("/sdcard/blue_main.png", PathEnum.IMAGE_OUTER + "main.png");
                situationArrayList.forEach(situation -> futureArrayList.add(completionService.submit(new SituationCallable(situation))));
                for (Future<Situation> situationFuture : futureArrayList) {
                    Situation curSituation = situationFuture.get();
                    log.debug("{}相似度为:{}", curSituation.getName(), curSituation.getSimile());
                    //如果当前情景相似度大于最低相似度,则进入判断
                    //如果当前对象的优先级比结果对象的优先级高则直接将结果设置为当前对象
                    //或者优先级相同,则比较当前对象的相似度和结果对象的相似度,哪一方高就为哪一方
                    if (curSituation.getSimile().compareTo(BigDecimal.valueOf(0.9)) >= 0
                            && (curSituation.getPriority() > endSituation.getPriority()
                            || (curSituation.getPriority().equals(endSituation.getPriority())
                            && curSituation.getSimile().compareTo(endSituation.getSimile()) >= 0))) {
                        endSituation = curSituation.copy();
                    }
                }
            } catch (InterruptedException | ExecutionException e) {
                Thread.currentThread().interrupt();
            }
            log.info("匹配结果为:{},相似度为:{}", endSituation.getName(), endSituation.getSimile().setScale(2, BigDecimal.ROUND_HALF_UP).toString());
            if (endSituation.getName().equals("匹配失败")) isMatch = false;
            else isMatch = true;
            waitTime = System.currentTimeMillis() - initMillis;
            log.debug("等待时间:{}ms", waitTime);
        }
        log.info("{}运行结束", mode.getName());
        return true;
    }

    private void clearSituation(Situation endSituation) {
        endSituation.setName("匹配失败");
        endSituation.setPriority(-1);
        endSituation.setImage(null);
        endSituation.setClick(false);
        endSituation.setAction(null);
        endSituation.setSimile(BigDecimal.valueOf(-1));
    }
}
