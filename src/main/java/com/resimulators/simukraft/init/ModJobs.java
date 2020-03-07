package com.resimulators.simukraft.init;

import com.resimulators.simukraft.common.entity.sim.EntitySim;
import com.resimulators.simukraft.common.jobs.JobBuilder;
import com.resimulators.simukraft.common.jobs.JobMiner;
import com.resimulators.simukraft.common.jobs.core.IJob;
import com.google.common.collect.ImmutableMap;

import java.util.Map;
import java.util.function.Function;

public class ModJobs {
    public static final Map<String, Function<EntitySim, IJob>> JOB_LOOKUP =new ImmutableMap.Builder<String, Function<EntitySim, IJob>>()
            .put("builder", JobBuilder::new)
            .put("miner", JobMiner::new)


            .build();
}