/*
 * Copyright 2024-2025 NetCracker Technology Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.qubership.itool.tasks.repository;

import java.util.Collection;

import javax.annotation.Nullable;
import javax.annotation.Resource;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.qubership.itool.modules.git.GitAdapter;
import org.qubership.itool.utils.ConfigUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.qubership.itool.tasks.AbstractAggregationTaskVerticle;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.impl.future.SucceededFuture;

import static org.qubership.itool.utils.ConfigProperties.RELEASE_BRANCH_POINTER;

public class RepositoriesCommitResultsVerticle extends AbstractAggregationTaskVerticle {
    protected Logger LOGGER = LoggerFactory.getLogger(RepositoriesCommitResultsVerticle.class);

    @Resource
    @Nullable
    private GitAdapter gitAdapter;

    @Override
    protected String[] features() {
        return new String[]{"repositoryUpdate", "unskippable"};
    }

    @Override
    protected void taskStart(Promise<?> taskPromise) {
        if (gitAdapter == null) {
            getLogger().warn("Offline mode. Exitting.");
            taskCompleted(taskPromise);
            return;
        }

        gitAdapter.openSuperrepository()
                .compose(repo -> gitAdapter.gitAdd(repo, ".")
                        .compose(r -> gitAdapter.gitStatus(repo))
                        .compose(status ->  {
                            Collection<String> missing = status.getMissing();
                            Future<Void> commitFuture = SucceededFuture.EMPTY;
                            String commitMessage = "Automatic commit on '"
                                    + ConfigUtils.getConfigValue(RELEASE_BRANCH_POINTER, config())
                                    + "' snapshot";
                            if (missing.isEmpty()) {
                                if (!status.isClean()) {
                                    commitFuture = gitAdapter.gitCommit(repo, commitMessage);
                                }
                            } else {
                                commitFuture = gitAdapter.gitRm(repo, missing)
                                        .compose(r -> gitAdapter.gitCommit(repo, commitMessage));
                            }
                            return commitFuture;
                        })
                )
                .onSuccess(r -> taskCompleted(taskPromise))
                .onFailure(r -> {
                    report.internalError("Commit in super repository failed: " + ExceptionUtils.getStackTrace(r));
                    taskCompleted(taskPromise);
                });
    }

    @Override
    protected Logger getLogger() {
        return LOGGER;
    }
}
