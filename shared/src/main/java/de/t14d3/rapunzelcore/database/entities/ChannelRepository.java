package de.t14d3.rapunzelcore.database.entities;

import de.t14d3.rapunzelcore.database.CoreDatabase;
import de.t14d3.spool.repository.EntityRepository;

public class ChannelRepository extends EntityRepository<Channel> {
    private static final ChannelRepository instance = new ChannelRepository();

    private ChannelRepository() {
        super(CoreDatabase.getEntityManager(), Channel.class);
    }

    public static ChannelRepository getInstance() {
        return instance;
    }
}

