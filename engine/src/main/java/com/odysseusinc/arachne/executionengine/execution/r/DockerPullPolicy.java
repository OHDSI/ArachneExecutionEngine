package com.odysseusinc.arachne.executionengine.execution.r;

public enum DockerPullPolicy {
    /** Never pull any images. If image does not exist in local registry, fail analysis. */
    NEVER,
    /** If image exists in local repository, use it. Do not check for updated image. Pull if image is missing.*/
    MISSING,
    /** Always attempt to pull. If pull failed but image exists in local repository, proceed with that image. */
    ALWAYS,
    /** Always attempt to pull. If pull fails, analysis will fail as well, even if local image exists. */
    FORCE,
}
