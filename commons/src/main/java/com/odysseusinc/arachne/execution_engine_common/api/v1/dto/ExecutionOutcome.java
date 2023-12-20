package com.odysseusinc.arachne.execution_engine_common.api.v1.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ExecutionOutcome {
    /**
     * A short string representing the stage on which execution is concluded. Some of possible values listed as constants under
     * {@link Stage} interface, however consumers should never assume this is a closed lies.
     * New falues might be added in the future.
     */
    private String stage;
    /**
     * A descriptive representation for the error, intended to be useful to humans.
     * Present only for failures, null for successful completion.
     */
    private String error;
    private String stdout;

    public ExecutionOutcome addError(String message) {
        // Note that while stage is not changed, this operation will add error, even to COMPLETED outcome
        // Therefore, it is possible to have COMPLETED analysis with an error. This is intended.
        String error = StringUtils.join(new String[]{getError(), message}, "; ");
        return new ExecutionOutcome(getStage(), error, getStdout());
    }
}
