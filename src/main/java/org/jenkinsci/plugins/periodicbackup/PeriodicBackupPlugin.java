package org.jenkinsci.plugins.periodicbackup;

import hudson.Plugin;
import hudson.logging.LogRecorder;
import hudson.logging.LogRecorder.Target;
import hudson.model.Hudson;
import hudson.security.ACL;

import java.io.IOException;
import java.util.logging.Level;

import org.acegisecurity.Authentication;
import org.acegisecurity.context.SecurityContextHolder;

/**
 * PeriodicBackupPlugin is responsible to set up and shutdown this plug-in.
 */
public class PeriodicBackupPlugin extends Plugin {

  /**
   * We want logs from all classes to same package log file.
   */
  private static final String[] DEBUG_LOGGER_NAMES = {
      "org.jenkinsci.plugins.periodicbackup"
  };

  /*
   * (non-Javadoc)
   * 
   * @see hudson.Plugin#start()
   */
  @Override
  public void start() {
    updateDebugLogger(Level.ALL, PeriodicBackupPlugin.DEBUG_LOGGER_NAMES);
  }

  /**
   * Checks if the specified debug loggers are installed on the Hudson server
   * and installs or updates the debug loggers according to the specified Level
   * if necessary. The will be one LogRecorder per debug logger.
   *
   * @param level The log Level of debug logger.
   * @param debugLoggerNames The names of the debug loggers.
   */
  private void updateDebugLogger(final Level level, final String... debugLoggerNames) {
    if (debugLoggerNames == null) {
      return;
    }

    // System rights are required for logger update.
    Authentication oldAuth = SecurityContextHolder.getContext().getAuthentication();
    SecurityContextHolder.getContext().setAuthentication(ACL.SYSTEM);

    try {
      for (String current : debugLoggerNames) {
        LogRecorder recorder = Hudson.getInstance().getLog().getLogRecorder(current);

        if (recorder == null) {
          Hudson.getInstance().getLog().doNewLogRecorder(current);
          recorder = Hudson.getInstance().getLog().getLogRecorder(current);
          recorder.targets.add(new Target(current, level));

          try {
            recorder.save();
          } catch (IOException ex) {
            System.err.println("An error occured while saving logger recorder!");
          }

          continue;
        }

        // Find Target with the specified name according to LogRecorder.
        Target target = null;
        if ((recorder != null) && (current != null)) {
          for (Target currTarg : recorder.targets) {
            if (currTarg.name.equals(current)) {
              target = currTarg;
              break;
            }
          }
        }

        if (target == null) {
          recorder.targets.add(new Target(current, level));
          try {
            recorder.save();
          } catch (IOException ex) {
            System.err.println("An error occured while saving logger recorder!");
          }
        }

      }
    } finally {
      SecurityContextHolder.getContext().setAuthentication(oldAuth);
    }
  }

}
