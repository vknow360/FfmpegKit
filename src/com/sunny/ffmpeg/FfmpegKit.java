package com.sunny.ffmpeg;

import android.net.Uri;
import com.arthenica.ffmpegkit.*;
import com.google.appinventor.components.annotations.DesignerComponent;
import com.google.appinventor.components.annotations.SimpleEvent;
import com.google.appinventor.components.annotations.SimpleFunction;
import com.google.appinventor.components.annotations.SimpleProperty;
import com.google.appinventor.components.runtime.ComponentContainer;
import com.google.appinventor.components.runtime.AndroidNonvisibleComponent;
import com.google.appinventor.components.runtime.EventDispatcher;
import com.google.appinventor.components.runtime.util.OnInitializeListener;
import com.sunny.util.NativeUtil;

import java.io.File;

@DesignerComponent(version = 1,
        versionName = "1.0",
        description = "Free and open-source extension for FFmpeg <br> Developed by Sunny Gupta",
        iconName = "https://res.cloudinary.com/andromedaviewflyvipul/image/upload/c_scale,h_20,w_20/v1571472765/ktvu4bapylsvnykoyhdm.png")
public class FfmpegKit extends AndroidNonvisibleComponent implements OnInitializeListener {
    public static boolean nativeLoaded = false;

    public FfmpegKit(ComponentContainer container) {
        super(container.$form());
        form.registerForOnInitialize(this);
    }

    @SimpleProperty(description = "Returns FFmpeg version")
    public String FFmpegVersion(){
        return FFmpegKitConfig.getFFmpegVersion();
    }

    @SimpleFunction(description = "Convert Storage Access Framework (SAF) Uri into path that can be read by FFmpegKit")
    public String GetSafParameterForRead(String safUri){
        return FFmpegKitConfig.getSafParameterForRead(form, Uri.parse(safUri));
    }

    @SimpleFunction(description = "Convert Storage Access Framework (SAF) Uri into path that can be written by FFmpegKit")
    public String GetSafParameterForWrite(String safUri){
        return FFmpegKitConfig.getSafParameterForWrite(form, Uri.parse(safUri));
    }

    @SimpleFunction(description = "Convert Storage Access Framework (SAF) Uri into path that can be read or written by FFmpegKit")
    public String GetSafParameter(String safUri, String openMode){
        return FFmpegKitConfig.getSafParameter(form, Uri.parse(safUri),openMode);
    }

    @SimpleFunction(description = "Tries to load native libraries from zip. 'zipPath' can be asset name, file path or direct url.")
    public void LoadLibraries(String zipPath) {
        if (!nativeLoaded) {
            NativeUtil nativeUtil = NativeUtil.getInstance(form, new NativeUtil.Callbacks() {
                @Override
                public void onError(final String errorMessage) {
                    form.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            ErrorOccurred(errorMessage);
                        }
                    });
                }

                @Override
                public void onDownloadProgress(int progress) {

                }

                @Override
                public void onDownloadComplete(File zipFile) {

                }

                @Override
                public void onLibsLoaded(String[] libPaths) {
                    nativeLoaded = true;
                    LibrariesLoaded();
                }
            }).usePreferredABI();
            if (zipPath.startsWith("http")) {
                nativeUtil.loadFromUrl(zipPath);
            } else if (zipPath.startsWith("//")) {
                nativeUtil.loadFromAssets(zipPath.substring(2));
            } else {
                nativeUtil.loadFromFile(new File(zipPath));
            }
        } else {
            LibrariesLoaded();
        }
    }

    @SimpleFunction(description = "Executes given command")
    public void Execute(String command){
        FFmpegSession session = FFmpegKit.execute(command);
        if (ReturnCode.isSuccess(session.getReturnCode())){
            CommandSuccess(true,session.toString());
        } else if (ReturnCode.isCancel(session.getReturnCode())) {
            CommandCancelled(true,session.toString());
        }else {
            CommandFailed(true,session.toString());
        }
    }

    @SimpleFunction(description = "Executes given command asynchronously")
    public void ExecuteAsync(String command){
        FFmpegKit.executeAsync(command, new FFmpegSessionCompleteCallback() {
            @Override
            public void apply(final FFmpegSession session) {
                final String output = session.toString();
                form.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (ReturnCode.isSuccess(session.getReturnCode())){
                            CommandSuccess(true,output);
                        } else if (ReturnCode.isCancel(session.getReturnCode())) {
                            CommandCancelled(true,output);
                        }else {
                            CommandFailed(true,output);
                        }
                    }
                });
            }
        }, new LogCallback() {
            @Override
            public void apply(final Log log) {
                form.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        GotNewLog(log.getSessionId(),log.getMessage());
                    }
                });
            }
        }, new StatisticsCallback() {
            @Override
            public void apply(final Statistics statistics) {
                form.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        GotNewStatistics(statistics.getSessionId(),statistics.toString());
                    }
                });
            }
        });
    }

    @SimpleEvent(description = "Event raised when new log is obtained in async execution")
    public void GotNewLog(long sessionId, String message){
        EventDispatcher.dispatchEvent(this,"GotNewLog",sessionId, message);
    }

    @SimpleEvent(description = "Event raised after getting new stats in async execution")
    public void GotNewStatistics(long sessionId, String statistics){
        EventDispatcher.dispatchEvent(this,"GotNewStatistics",sessionId, statistics);
    }

    @SimpleEvent(description = "Event raised when previous command was executed successfully")
    public void CommandSuccess(boolean async,String output){
        EventDispatcher.dispatchEvent(this,"CommandSuccess",async,output);
    }
    @SimpleEvent(description = "Event raised when previous command was cancelled")
    public void CommandCancelled(boolean async, String output){
        EventDispatcher.dispatchEvent(this,"CommandCancelled",async,output);
    }
    @SimpleEvent(description = "Event raised when previous command failed to execute")
    public void CommandFailed(boolean async, String output){
        EventDispatcher.dispatchEvent(this,"CommandFailed",async,output);
    }

    @SimpleEvent(description = "Event raised when any error occurs.")
    public void ErrorOccurred(final String errorMsg) {
        EventDispatcher.dispatchEvent(this, "ErrorOccurred", errorMsg);
    }

    @SimpleEvent(description = "Event raised when native libraries have been loaded successfully")
    public void LibrariesLoaded() {
        EventDispatcher.dispatchEvent(this, "LibrariesLoaded");
    }

    @Override
    public void onInitialize() {

    }
}