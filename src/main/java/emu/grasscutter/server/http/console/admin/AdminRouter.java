package emu.grasscutter.server.http.console.admin;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import emu.grasscutter.Grasscutter;
import emu.grasscutter.utils.JsonUtils;
import io.javalin.Javalin;
import io.javalin.http.Context;
import java.util.Map;
import java.util.function.Predicate;

/**
 * 管理页 JSON 接口的公共约定。
 *
 * <p>路径统一挂在 {@code /console/api/admin/} 下；请求体是 JSON 对象，回包是 JSON 对象。出错时回
 * {@code {"error": "中文原因"}}：参数不对 400，状态冲突（如玩家不在线、正被其他操作锁住）409，
 * 其他 500。鉴权沿用控制台的 {@code X-Console-Token}，由外层传进来的 {@code authorized} 判断。
 */
public final class AdminRouter {
    public static final String PREFIX = "/console/api/admin/";

    @FunctionalInterface
    public interface Handler {
        Object handle(Request request) throws Exception;
    }

    /** 一次请求：查询参数和 JSON 请求体的取值助手，缺必填项时抛 IllegalArgumentException。 */
    public record Request(Context ctx, JsonObject body) {
        public String query(String name) {
            return ctx.queryParam(name);
        }

        public int queryInt(String name, int fallback) {
            var value = ctx.queryParam(name);
            if (value == null || value.isBlank()) return fallback;
            try {
                return Integer.parseInt(value.trim());
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("参数 " + name + " 必须是整数");
            }
        }

        public boolean has(String name) {
            return body.has(name) && !body.get(name).isJsonNull();
        }

        public JsonElement get(String name) {
            if (!has(name)) throw new IllegalArgumentException("缺少参数 " + name);
            return body.get(name);
        }

        public String string(String name) {
            var value = get(name).getAsString();
            if (value.isBlank()) throw new IllegalArgumentException("参数 " + name + " 不能为空");
            return value;
        }

        public String string(String name, String fallback) {
            return has(name) ? body.get(name).getAsString() : fallback;
        }

        public int integer(String name) {
            try {
                return get(name).getAsInt();
            } catch (NumberFormatException | UnsupportedOperationException e) {
                throw new IllegalArgumentException("参数 " + name + " 必须是整数");
            }
        }

        public int integer(String name, int fallback) {
            return has(name) ? integer(name) : fallback;
        }

        public long longValue(String name, long fallback) {
            try {
                return has(name) ? body.get(name).getAsLong() : fallback;
            } catch (NumberFormatException | UnsupportedOperationException e) {
                throw new IllegalArgumentException("参数 " + name + " 必须是整数");
            }
        }

        public boolean bool(String name, boolean fallback) {
            return has(name) ? body.get(name).getAsBoolean() : fallback;
        }

        public String address() {
            return ctx.ip();
        }
    }

    private final Javalin javalin;
    private final Predicate<Context> authorized;

    public AdminRouter(Javalin javalin, Predicate<Context> authorized) {
        this.javalin = javalin;
        this.authorized = authorized;
    }

    public void get(String path, Handler handler) {
        javalin.get(PREFIX + path, ctx -> run(ctx, path, handler));
    }

    public void post(String path, Handler handler) {
        javalin.post(PREFIX + path, ctx -> run(ctx, path, handler));
    }

    private void run(Context ctx, String path, Handler handler) {
        if (!authorized.test(ctx)) return;
        JsonObject body;
        try {
            var text = ctx.body();
            body = text == null || text.isBlank() ? new JsonObject() : JsonParser.parseString(text).getAsJsonObject();
        } catch (Exception e) {
            json(ctx, 400, Map.of("error", "请求体不是 JSON 对象"));
            return;
        }
        try {
            var result = handler.handle(new Request(ctx, body));
            json(ctx, 200, result == null ? Map.of() : result);
        } catch (IllegalStateException e) {
            json(ctx, 409, Map.of("error", message(e)));
        } catch (IllegalArgumentException | NullPointerException | ClassCastException | UnsupportedOperationException e) {
            json(ctx, 400, Map.of("error", message(e)));
        } catch (Exception e) {
            Grasscutter.getLogger().warn("Admin API {} failed", path, e);
            json(ctx, 500, Map.of("error", "操作失败，请查看服务端日志：" + e));
        }
    }

    private static String message(Exception e) {
        return e.getMessage() == null ? "请求参数缺失或格式不正确" : e.getMessage();
    }

    static void json(Context ctx, int status, Object body) {
        ctx.status(status);
        ctx.contentType("application/json; charset=UTF-8");
        ctx.result(JsonUtils.encode(body));
    }
}
