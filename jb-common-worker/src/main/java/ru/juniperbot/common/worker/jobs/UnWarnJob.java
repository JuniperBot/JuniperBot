/*
 * This file is part of JuniperBot.
 *
 * JuniperBot is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.

 * JuniperBot is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License
 * along with JuniperBot. If not, see <http://www.gnu.org/licenses/>.
 */
package ru.juniperbot.common.worker.jobs;

import lombok.NonNull;
import org.quartz.*;
import org.springframework.beans.factory.annotation.Autowired;
import ru.juniperbot.common.persistence.entity.MemberWarning;
import ru.juniperbot.common.persistence.repository.MemberWarningRepository;
import ru.juniperbot.common.service.TransactionHandler;
import ru.juniperbot.common.worker.shared.support.AbstractJob;

public class UnWarnJob extends AbstractJob {

    public static final String ATTR_WARNING_ID = "warningId";
    public static final String GROUP = "UnWarnJob-group";

    @Autowired
    private MemberWarningRepository repository;

    @Autowired
    private TransactionHandler transactionHandler;

    @Override
    public void execute(JobExecutionContext jobExecutionContext) {
        JobDataMap data = jobExecutionContext.getJobDetail().getJobDataMap();
        long warningId = data.getLongFromString(ATTR_WARNING_ID);
        transactionHandler.runInTransaction(() -> repository.flushWarning(warningId));
    }

    public static JobDetail createDetails(@NonNull MemberWarning warning) {
        return JobBuilder
                .newJob(UnWarnJob.class)
                .withIdentity(getKey(warning))
                .usingJobData(ATTR_WARNING_ID, String.valueOf(warning.getId()))
                .build();
    }

    public static JobKey getKey(MemberWarning warning) {
        return new JobKey(String.format("%s-%s", GROUP, warning.getId()), GROUP);
    }
}
