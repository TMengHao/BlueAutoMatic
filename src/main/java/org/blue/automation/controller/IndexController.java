package org.blue.automation.controller;

import javafx.beans.property.Property;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.blue.automation.Main;
import org.blue.automation.entities.Mode;
import org.blue.automation.entities.enums.PathEnum;
import org.blue.automation.factories.UIControlFactory;
import org.blue.automation.services.ModeService;
import org.blue.automation.services.OperationService;
import org.blue.automation.services.impl.AdbOperationServiceImpl;
import org.blue.automation.services.impl.ModeServiceImpl;
import org.blue.automation.services.impl.PCOperationServiceImpl;
import org.blue.automation.thread.ModeCallable;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

/**
 * name: MengHao Tian
 * date: 2022/4/24 15:08
 */
public class IndexController implements Initializable {
    private static final Logger log = LogManager.getLogger(IndexController.class);
    @FXML
    private ChoiceBox<Mode> CHOICE_MODE_LIST;
    @FXML
    private ChoiceBox<String> CHOICE_OPERATION_LIST;
    @FXML
    private Button BUTTON_CHOOSE_ADB_FILE;

    @FXML
    private Button BUTTON_SWITCH;

    /**
     * 当前模式属性
     **/
    private static final SimpleObjectProperty<Mode> CURRENT_MODE_PROPERTY = new SimpleObjectProperty<>();
    /**
     * 模式接口
     **/
    private static final ModeService MODE_SERVICE = new ModeServiceImpl("main.json");
    // TODO: 2022/5/1 添加可选择启动模式的功能
    /**
     * 操作接口
     **/
    private static OperationService OPERATION_SERVICE;
    /**
     * 模式列表
     **/
    private final Property<ObservableList<Mode>> modeList = new SimpleListProperty<>();
    /**
     * 主线程池
     **/
    private final ExecutorService THREAD_POOL = Main.THREAD_POOL;
    /**
     * 正在运行的模式线程
     **/
    private Future<Boolean> runningMode;


    @Override
    public void initialize(URL location, ResourceBundle resources) {
        ArrayList<Mode> modes = MODE_SERVICE.selectAllModes();
        modeList.setValue(FXCollections.observableArrayList(modes));
        //模式数组与模式下拉列表控件双向绑定
        CHOICE_MODE_LIST.itemsProperty().bindBidirectional(modeList);
        CHOICE_MODE_LIST.setConverter(new StringConverter<Mode>() {
            @Override
            public String toString(Mode object) {
                return object.getName();
            }

            @Override
            public Mode fromString(String string) {
                return new Mode().setName(string);
            }
        });
        //当前模式对象与模式下拉列表选中模式绑定
        CHOICE_MODE_LIST.valueProperty().bindBidirectional(CURRENT_MODE_PROPERTY);
        CHOICE_MODE_LIST.getSelectionModel().selectFirst();
        //初始化操作方式下拉列表
        CHOICE_OPERATION_LIST.getItems().addAll("手机","模拟器","电脑");
        CHOICE_OPERATION_LIST.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            switch (newValue){
                case "手机":
                case "模拟器":
                    OPERATION_SERVICE = new AdbOperationServiceImpl();
                    BUTTON_CHOOSE_ADB_FILE.setDisable(false);
                    break;
                case "电脑":
                    OPERATION_SERVICE = new PCOperationServiceImpl();
                    BUTTON_CHOOSE_ADB_FILE.setDisable(true);
                    break;
            }
        });
        CHOICE_OPERATION_LIST.getSelectionModel().select("手机");
    }

    @FXML
    void chooseAdbFile() {
        File file = UIControlFactory.createFileChooser("选择json文件", PathEnum.JSON.getPath(),false).showOpenDialog(Main.STAGE_MAP.get("primaryStage"));
        if(file != null) OPERATION_SERVICE.setFilePath(file.getAbsolutePath());
    }

    /**
     * 添加模式
     **/
    @FXML
    private void addMode() {
        TextInputDialog dialog = UIControlFactory.createTestInputDialog("添加模式", null, "请输入模式名称");
        Optional<String> nameText = dialog.showAndWait();
        nameText.ifPresent(name -> {
            Mode mode = new Mode().setName(name);
            if(MODE_SERVICE.addMode(mode)) {
                modeList.setValue(FXCollections.observableArrayList(MODE_SERVICE.selectAllModes()));
                CURRENT_MODE_PROPERTY.set(mode);
                log.info("{}添加成功",name);
            }else{
                new Alert(Alert.AlertType.ERROR,"添加失败").showAndWait();
                log.error("{}添加失败", name);
            }
        });
    }

    /**
     * 打开配置模式页面
     **/
    @FXML
    private void configureMode() {
        Stage settingStage = Main.STAGE_MAP.get("modeSettingStage");
        if (settingStage == null) {
            settingStage = new Stage();
            settingStage.setTitle("模式配置");
            settingStage.setResizable(false);
            settingStage.setOnCloseRequest(event -> CURRENT_MODE_PROPERTY.set(MODE_SERVICE.selectModeByName(CURRENT_MODE_PROPERTY.get().getName())));
            Main.STAGE_MAP.put("modeSettingStage", settingStage);
        }
        try {
            settingStage.setScene(new Scene(new FXMLLoader(getClass().getResource("/views/modeSetting.fxml")).load(), 600, 400));
            settingStage.show();
        } catch (IOException e) {
            log.error("创建settingStage异常:", e);
        }
    }

    /**
     * 删除模式
     **/
    @FXML
    private void deleteMode() {
        if(MODE_SERVICE.deleteModeByName(CURRENT_MODE_PROPERTY.get().getName())){
            log.info("{}模式删除成功", CURRENT_MODE_PROPERTY.get().getName());
            modeList.setValue(FXCollections.observableArrayList(MODE_SERVICE.selectAllModes()));
            CURRENT_MODE_PROPERTY.set(modeList.getValue().get(0));
        }else{
            log.error("{}模式删除失败", CURRENT_MODE_PROPERTY.get().getName());
        }
    }

    /**
     * 运行按钮设置
     **/
    @FXML
    private void switchOnAndOff() {
        BUTTON_SWITCH.setDisable(true);
        switch (BUTTON_SWITCH.getText()) {
            case "运行":
                log.debug("当前模式:{}", CURRENT_MODE_PROPERTY);
                ModeCallable modeCallable = new ModeCallable();
                runningMode = THREAD_POOL.submit(modeCallable);
                BUTTON_SWITCH.setText("结束");
                break;
            case "结束":
                if (!runningMode.isDone()) runningMode.cancel(true);
                BUTTON_SWITCH.setText("运行");
                break;
        }
        BUTTON_SWITCH.setDisable(false);
    }

    /**
     * 导出配置文件
     **/
    @FXML
    public void exportFile() {

    }

    /**
     * 导入配置文件
     **/
    @FXML
    public void importFile() {

    }

    /**
     * 打开帮助界面
     **/
    @FXML
    public void openHelp() {
        log.info("打开帮助界面");
    }

    public static Mode getCurrentMode() {
        return CURRENT_MODE_PROPERTY.get();
    }

    public static ModeService getModeService() {
        return MODE_SERVICE;
    }

    public static OperationService getOperationService() {
        return OPERATION_SERVICE;
    }
}
