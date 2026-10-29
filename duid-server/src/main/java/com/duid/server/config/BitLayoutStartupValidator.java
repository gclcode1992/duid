package com.duid.server.config;

import com.duid.common.layout.BitLayoutProperties;
import com.duid.common.layout.BitLayoutValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * 服务启动时执行位布局自检，配置不合法则拒绝启动。
 *
 * @author gclcode@126.com
 */
@Component
public class BitLayoutStartupValidator implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(BitLayoutStartupValidator.class);

    private final BitLayoutProperties layout;

    public BitLayoutStartupValidator(BitLayoutProperties layout) {
        this.layout = layout;
    }

    @Override
    public void run(ApplicationArguments args) {
        BitLayoutValidator.validate(layout);
        log.info("Bit layout validated: timestamp={} worker={} sequence={} rollback={} (total={})",
                layout.timestampBits(), layout.workerBits(), layout.sequenceBits(),
                layout.rollbackBits(), layout.totalBits());
    }
}
