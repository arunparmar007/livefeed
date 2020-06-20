package livefeeddisplay;

import static j2html.TagCreator.*;

import java.util.HashMap;


import spark.Spark;

/**
 * @author arun.parmar 20-Jun-2020 livefeeddisplay
 */
public class httpserver {
	private static String KeyPattern;
	private static String[] dataChangedCasesHandled;

	public static void main(String[] args) {
		KeyPattern = "counter_";
		dataChangedCasesHandled = "TestCounter1,TestCounter2".split(",");
		Spark.webSocket("/websocket/livefeed", WebSocketHelper.class);
		Spark.get("/", (request, response) -> {
			return "live feed Server is up!";
		});
		new Thread(() -> {
			RedisHelper.getInstance().subscribe("counter_*");
		}).start();
		Spark.get("/livefeeddisplay", (request, response) -> {
			HashMap<String, Object> model = new HashMap<String, Object>();
			return getJ2HTML();
		});
		Spark.init();

	}

	public static String getJ2HTML() {
		HashMap<String, String> counterValues = RedisHelper.getInstance().getCurrentRedisCounterValues();
		HashMap<String, Boolean> inventoryChangedCasesHandledMap = new HashMap<>();
        for (String s : dataChangedCasesHandled) {
            inventoryChangedCasesHandledMap.put(s, true);
        }
		String html = html(head(title("livefeed")),
				body(h2("REAL TIME DISPLAY"),
						div(
                                each(inventoryChangedCasesHandledMap.keySet(), key ->
                                                div(key + ": " + counterValues.get(KeyPattern + key))
                                                
                                        )
                                ).withId("msg-box").withStyle("padding: 10px; width:250px; height: 100px; background: #eee; overflow:auto;"),
                        
						scriptWithInlineFile("javascript/WebSocket.js")

				).withStyle("margin: 30px")).withStyle("color: blue").render();
		return html;
	}

}
