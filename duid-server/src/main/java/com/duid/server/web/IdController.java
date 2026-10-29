package com.duid.server.web;

import com.duid.common.encoding.Base36Upper;
import com.duid.common.encoding.Base62;
import com.duid.common.util.PrefixUtils;
import com.duid.server.service.IdGeneratorService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * DUID 对外 HTTP 接口。
 * <p>
 * 提供 long 型数字 ID 及 Base62 / Base36 大写压缩 ID，支持可选前缀参数。
 * </p>
 *
 * @author gclcode@126.com
 */
@RestController
public class IdController {

    private final IdGeneratorService idGeneratorService;

    public IdController(IdGeneratorService idGeneratorService) {
        this.idGeneratorService = idGeneratorService;
    }

    /**
     * 获取下一个全局唯一 ID，响应体为纯十进制数字字符串。
     */
    @GetMapping(value = "/long-id", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> getLongId() {
        return textId(Long.toString(idGeneratorService.nextId()), "");
    }

    /**
     * 获取带前缀的全局唯一 ID，响应体为「前缀 + 十进制数字」。
     *
     * @param prefix 前缀，null 或空字符串视为无前缀
     */
    @GetMapping(value = "/long-id", params = "prefix", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> getLongIdWithPrefix(@RequestParam(required = false) String prefix) {
        return textId(Long.toString(idGeneratorService.nextId()), prefix);
    }

    /**
     * 获取 Base62 压缩 ID（0-9、A-Z、a-z）。
     */
    @GetMapping(value = "/base62-id", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> getBase62Id() {
        return textId(Base62.encode(idGeneratorService.nextId()), "");
    }

    /**
     * 获取带前缀的 Base62 压缩 ID。
     *
     * @param prefix 前缀，null 或空字符串视为无前缀
     */
    @GetMapping(value = "/base62-id", params = "prefix", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> getBase62IdWithPrefix(@RequestParam(required = false) String prefix) {
        return textId(Base62.encode(idGeneratorService.nextId()), prefix);
    }

    /**
     * 获取 Base36 大写压缩 ID（仅 0-9、A-Z）。
     */
    @GetMapping(value = "/base36-upper-id", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> getBase36UpperId() {
        return textId(Base36Upper.encode(idGeneratorService.nextId()), "");
    }

    /**
     * 获取带前缀的 Base36 大写压缩 ID。
     *
     * @param prefix 前缀，null 或空字符串视为无前缀
     */
    @GetMapping(value = "/base36-upper-id", params = "prefix", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> getBase36UpperIdWithPrefix(@RequestParam(required = false) String prefix) {
        return textId(Base36Upper.encode(idGeneratorService.nextId()), prefix);
    }

    /**
     * 就绪探针：已获取 workerId 且发号器可用时返回 200。
     */
    @GetMapping("/ready")
    public ResponseEntity<Void> ready() {
        if (!idGeneratorService.isReady()) {
            return ResponseEntity.status(503).build();
        }
        return ResponseEntity.ok().build();
    }

    private ResponseEntity<String> textId(String id, String prefix) {
        if (!idGeneratorService.isReady()) {
            return ResponseEntity.status(503).build();
        }
        return ResponseEntity.ok(PrefixUtils.withPrefix(prefix, id));
    }
}
