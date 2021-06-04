/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License;
 * you may not use this file except in compliance with the Elastic License.
 */
package com.elastic.support.scrub;

import com.elastic.support.Constants;
import com.elastic.support.util.SystemProperties;
import com.elastic.support.util.TaskEntry;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.util.concurrent.Callable;

public class ScrubTask implements Callable<String> {

    private static Logger logger = LogManager.getLogger(ScrubTask.class);
    ScrubProcessor processor;
    TaskEntry entry;
    String dir;

    public ScrubTask(ScrubProcessor processor, TaskEntry entry, String dir){
        this.entry = entry;
        this.processor = processor;
        this.dir = dir;
    }

    @Override
    public String call() {
        String result;
        try {
            logger.debug(entry.entryName() + " started");

            // If it's in remove we not only don't process it we don't write it to the scrubbed archive either
            if (processor.isRemove(entry.entryName())) {
                logger.info(Constants.CONSOLE, "Removing entry: {}", entry.entryName());
                return entry.entryName() + ":removed";
            }

            String content = entry.content();

            if (processor.isExclude(entry.entryName())) {
                result = entry.entryName() + ":excluded";
                logger.info(Constants.CONSOLE, "Excluded from sanitization: {}", entry.entryName());

            } else {
                content = processor.processAutoscrub(content);
                content = processor.processContentWithTokens(content, entry.entryName());
                result = entry.entryName() + ":sanitized";
                logger.info(Constants.CONSOLE, "Processed entry: {}", entry.entryName());

            }

            String targetFileName = dir + SystemProperties.fileSeparator + entry.entryName();
            FileUtils.writeStringToFile(new File(targetFileName), content, "UTF-8");

        } catch (Exception e) {
            logger.error("Error occurrred processing: {}", entry.entryName(), e);
            result = "error:" + entry.entryName() + " " + e.getMessage();
        }

        logger.debug(entry.entryName() + " complete");

        return result;

    }

    @Override
    public String toString() {
        return super.toString() + entry.entryName() + ":incomplete";
    }

}
