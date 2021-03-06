package com.stream_pi.client.window;

import com.stream_pi.client.connection.ClientListener;
import com.stream_pi.client.io.Config;
import com.stream_pi.client.info.ClientInfo;

import java.io.File;
import java.util.logging.Logger;

import com.stream_pi.client.Main;
import com.stream_pi.client.profile.ClientProfiles;
import com.stream_pi.client.window.dashboard.DashboardBase;
import com.stream_pi.client.window.firsttimeuse.FirstTimeUse;
import com.stream_pi.client.window.settings.SettingsBase;
import com.stream_pi.theme_api.Theme;
import com.stream_pi.theme_api.Themes;
import com.stream_pi.util.alert.StreamPiAlert;
import com.stream_pi.util.combobox.StreamPiComboBox;
import com.stream_pi.util.exception.MinorException;
import com.stream_pi.util.exception.SevereException;
import com.stream_pi.util.iohelper.IOHelper;
import com.stream_pi.util.loggerhelper.StreamPiLogFallbackHandler;
import com.stream_pi.util.loggerhelper.StreamPiLogFileHandler;
import com.stream_pi.util.platform.Platform;

import javafx.application.HostServices;
import javafx.geometry.Insets;
import javafx.scene.CacheHint;
import javafx.scene.Cursor;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Font;
import javafx.stage.Stage;

public abstract class Base extends StackPane implements ExceptionAndAlertHandler, ClientListener {

    private Config config;

    private ClientProfiles clientProfiles;

    private ClientInfo clientInfo;

    private Stage stage;

    public Stage getStage()
    {
        return stage;
    }

    public Logger getLogger()
    {
        return logger;
    }

    private DashboardBase dashboardBase;
    private SettingsBase settingsBase;

    private FirstTimeUse firstTimeUse;

    public FirstTimeUse getFirstTimeUse() {
        return firstTimeUse;
    }
    

    private StackPane alertStackPane;

    @Override
    public ClientProfiles getClientProfiles() {
        return clientProfiles;
    }

    public void setClientProfiles(ClientProfiles clientProfiles) {
        this.clientProfiles = clientProfiles;
    }

    private Logger logger = null;
    private StreamPiLogFileHandler logFileHandler = null;
    private StreamPiLogFallbackHandler logFallbackHandler = null;

    @Override
    public void initLogger()
    {
        try
        {
            if(logger != null || logFileHandler != null)
                return;

            closeLogger();
            logger = Logger.getLogger("");

            if(new File(ClientInfo.getInstance().getPrePath()).getAbsoluteFile().getParentFile().canWrite())
            {

                String path = ClientInfo.getInstance().getPrePath()+"../streampi.log";

                if(ClientInfo.getInstance().getPlatformType() == Platform.ANDROID)
                    path = ClientInfo.getInstance().getPrePath()+"streampi.log";

                logFileHandler = new StreamPiLogFileHandler(path);
                logger.addHandler(logFileHandler);
            }
            else
            {
                logFallbackHandler = new StreamPiLogFallbackHandler();
                logger.addHandler(logFallbackHandler);
            }
            
        }
        catch(Exception e)
        {
            e.printStackTrace();

            logFallbackHandler = new StreamPiLogFallbackHandler();
            logger.addHandler(logFallbackHandler);
        }
    }
    
    public void closeLogger()
    {
        if(logFileHandler != null)
            logFileHandler.close();
        else if(logFallbackHandler != null)
            logFallbackHandler.close();
    }

    private HostServices hostServices;

    public void setHostServices(HostServices hostServices)
    {
        this.hostServices = hostServices;
    }

    public HostServices getHostServices()
    {
        return hostServices;
    }

    public void initBase() throws SevereException
    {
        stage = (Stage) getScene().getWindow();

        clientInfo = ClientInfo.getInstance();
        dashboardBase = new DashboardBase(this, this);
        dashboardBase.prefWidthProperty().bind(widthProperty());
        dashboardBase.prefHeightProperty().bind(heightProperty());

        settingsBase = new SettingsBase(this, this, getHostServices());

        alertStackPane = new StackPane();
        alertStackPane.setPadding(new Insets(10));
        alertStackPane.setVisible(false);

        StreamPiAlert.setParent(alertStackPane);
        StreamPiComboBox.setParent(alertStackPane);

        firstTimeUse = new FirstTimeUse(this, this);

        getChildren().clear();
        getChildren().addAll(alertStackPane);


        initLogger();

        checkPrePathDirectory();


        getChildren().addAll(settingsBase, dashboardBase);

        setStyle(null);

        config = Config.getInstance();


        if(config.isFirstTimeUse())
        {
            clearStylesheets();
            applyDefaultStylesheet();
            applyDefaultIconsStylesheet();

            getChildren().add(firstTimeUse);
            firstTimeUse.toFront();
        }
        else
        {
            dashboardBase.toFront();
        }

        initThemes();
    }

    private void checkPrePathDirectory() throws SevereException
    {
        try 
        {
            File filex = new File(ClientInfo.getInstance().getPrePath());

            if(filex.getAbsoluteFile().getParentFile().canWrite())
            {
                if(!filex.exists())
                { 
                    filex.mkdirs();
                    IOHelper.unzip(Main.class.getResourceAsStream("Default.obj"), ClientInfo.getInstance().getPrePath());
                }
            }
            else
            {
                if(getClientInfo().getPlatformType() != Platform.ANDROID)
                {
                    setPrefSize(300,300);
                }

                clearStylesheets();
                applyDefaultStylesheet();
                applyDefaultIconsStylesheet();
                getStage().show();
                throw new SevereException("No storage permission. Give it!");
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
            throw new SevereException(e.getMessage());
        }
    }

    public void setupFlags()
    {
        //Full Screen
        if(getConfig().isFullscreen())
        {
            getStage().setFullScreenExitKeyCombination(KeyCombination.NO_MATCH);
            getStage().setFullScreen(true);
        }
        else
        {
            getStage().setFullScreenExitKeyCombination(KeyCombination.keyCombination("Esc"));
            getStage().setFullScreen(false);
        }

        //Cursor
        if(getConfig().isShowCursor())
        {
            setCursor(Cursor.DEFAULT);
        }
        else
        {
            setCursor(Cursor.NONE);
        }
    }


    public SettingsBase getSettingsPane() {
        return settingsBase;
    }

    public DashboardBase getDashboardPane() {
        return dashboardBase;
    }

    public void renderRootDefaultProfile()
    {
        getDashboardPane().renderProfile(getClientProfiles().getProfileFromID(
                getConfig().getStartupProfileID()
        ), true);
    }



    public void clearStylesheets()
    {
        getStylesheets().clear();
    }



    public void applyDefaultStylesheet()
    {
        Font.loadFont(Main.class.getResourceAsStream("Roboto.ttf"), 13);
        getStylesheets().add(Main.class.getResource("style.css").toExternalForm());
    }

    public void applyDefaultIconsStylesheet()
    {
        Font.loadFont(Main.class.getResourceAsStream("Roboto.ttf"), 13);
        getStylesheets().add(Main.class.getResource("default_icons.css").toExternalForm());
    }


    public Config getConfig()
    {
        return config;
    }

    public ClientInfo getClientInfo()
    {
        return clientInfo;
    }

    private Theme currentTheme;

    @Override
    public Theme getCurrentTheme()
    {
        return currentTheme;
    }


    public void applyTheme(Theme t)
    {
        logger.info("Applying theme '"+t.getFullName()+"' ...");

        if(t.getFonts() != null)
        {
            for(String fontFile : t.getFonts())
            {
                Font.loadFont(fontFile.replace("%20",""), 13);
            }
        }
        currentTheme = t;

        clearStylesheets();
        applyDefaultStylesheet();
        getStylesheets().addAll(t.getStylesheets());
        applyDefaultIconsStylesheet();

        logger.info("... Done!");
    }

    Themes themes;
    public void initThemes() throws SevereException
    {
        logger.info("Loading themes ...");
        themes = new Themes(getConfig().getThemesPath(), getConfig().getCurrentThemeFullName(), clientInfo.getMinThemeSupportVersion());


        if(themes.getErrors().size()>0)
        {
            StringBuilder themeErrors = new StringBuilder();

            for(MinorException eachException : themes.getErrors())
            {
                themeErrors.append("\n * ").append(eachException.getShortMessage());
            }

            if(themes.getIsBadThemeTheCurrentOne())
            {
                themeErrors.append("\n\nReverted to default theme! (").append(getConfig().getDefaultCurrentThemeFullName()).append(")");

                getConfig().setCurrentThemeFullName(getConfig().getDefaultCurrentThemeFullName());
                getConfig().save();
            }

            handleMinorException(new MinorException("Theme Loading issues", themeErrors.toString()));
        }

        logger.info("... Done!");
    }

    @Override
    public Themes getThemes() {
        return themes;
    }



    public void applyDefaultTheme()
    {
        logger.info("Applying default theme ...");

        boolean foundTheme = false;
        for(Theme t: themes.getThemeList())
        {
            if(t.getFullName().equals(config.getCurrentThemeFullName()))
            {
                foundTheme = true;
                applyTheme(t);
                break;
            }
        }

        if(foundTheme)
        {
            logger.info("... Done!");
        }
        else
        {
            logger.info("Theme not found. reverting to light theme ...");
            try {
                Config.getInstance().setCurrentThemeFullName("com.StreamPi.DefaultLight");
                Config.getInstance().save();

                applyDefaultTheme();
            }
            catch (SevereException e)
            {
                handleSevereException(e);
            }
        }


    }

    @Override
    public String getDefaultThemeFullName()
    {
        return config.getCurrentThemeFullName();
    }


}
