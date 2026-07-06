package it.fin8.gdrsheet.config;

import net.ttddyy.dsproxy.ExecutionInfo;
import net.ttddyy.dsproxy.QueryInfo;
import net.ttddyy.dsproxy.listener.QueryExecutionListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.util.List;

/**
 * Tracciamento query SQL con stack walk: SOLO profilo "dev". Lo stack walk +
 * il logging per-query hanno un costo non trascurabile se eseguiti su ogni
 * query di ogni richiesta — non deve mai finire in produzione.
 */
@Configuration
@Profile("dev")
public class DatasourceProxyTraceConfig {

    private static final Logger log = LoggerFactory.getLogger(DatasourceProxyTraceConfig.class);

    @Bean
    public QueryExecutionListener queryExecutionListener() {
        return new QueryExecutionListener() {
            @Override
            public void beforeQuery(ExecutionInfo executionInfo, List<QueryInfo> queryInfoList) {
            }

            @Override
            public void afterQuery(ExecutionInfo executionInfo, List<QueryInfo> queryInfoList) {
                String caller = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE)
                        .walk(stream -> stream
                                .filter(f -> f.getClassName().startsWith("it.fin8.gdrsheet"))
                                .filter(f -> !f.getClassName().startsWith("it.fin8.gdrsheet.config"))
                                .findFirst()
                                .map(f -> f.getClassName() + "#" + f.getMethodName() + ":" + f.getLineNumber())
                                .orElse("caller non trovato"));

                log.info("SQL originata da {} -> {}", caller, queryInfoList);
            }
        };
    }
}