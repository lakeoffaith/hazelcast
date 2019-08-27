/*
 * Copyright (c) 2008-2019, Hazelcast, Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hazelcast.sql.impl.exec;

import com.hazelcast.sql.impl.expression.Expression;
import com.hazelcast.sql.impl.row.EmptyRowBatch;
import com.hazelcast.sql.impl.row.HeapRow;
import com.hazelcast.sql.impl.row.Row;
import com.hazelcast.sql.impl.row.RowBatch;

import java.util.List;

/**
 * Projection executor.
 */
public class ProjectExec extends AbstractUpstreamAwareExec {
    /** Projection expressions. */
    private final List<Expression> projections;

    /** Last upstream batch. */
    private RowBatch curBatch;

    /** Current position in the last upstream batch. */
    private int curBatchPos = -1;

    /** Current row. */
    private RowBatch curRow;

    public ProjectExec(Exec upstream, List<Expression> projections) {
        super(upstream);

        this.projections = projections;
    }

    @Override
    public IterationResult advance() {
        if (curBatch == null) {
            if (upstreamDone)
                return IterationResult.FETCHED_DONE;

            switch (advanceUpstream()) {
                case FETCHED_DONE:
                case FETCHED:
                    RowBatch batch = upstreamCurrentBatch;

                    if (batch.getRowCount() > 0) {
                        curBatch = batch;
                        curBatchPos = 0;
                    }

                    break;

                case WAIT:
                    return IterationResult.WAIT;

                default:
                    throw new IllegalStateException("Should not reach this.");
            }
        }

        return advanceCurrentBatch();
    }

    /**
     * Advance currently available batch.
     *
     * @return Result.
     */
    private IterationResult advanceCurrentBatch() {
        // TODO: Make sure that we do not perform unnecessary call to upstream.
        // TODO: To achieve this, upstream must set FETCHED_DONE when the last row is returned.
        if (curBatch == null) {
            assert upstreamDone;

            curRow = EmptyRowBatch.INSTANCE;

            return IterationResult.FETCHED_DONE;
        }

        Row upstreamRow = curBatch.getRow(curBatchPos);

        HeapRow curRow0 = new HeapRow(projections.size());

        int colIdx = 0;

        for (Expression projection : projections) {
            Object res = projection.eval(ctx, upstreamRow);

            curRow0.set(colIdx++, res);
        }

        curRow = curRow0;

        if (++curBatchPos == curBatch.getRowCount()) {
            curBatch = null;
            curBatchPos = -1;

            if (upstreamDone)
                return IterationResult.FETCHED_DONE;
        }

        return IterationResult.FETCHED;
    }

    @Override
    public RowBatch currentBatch() {
        return curRow;
    }
}