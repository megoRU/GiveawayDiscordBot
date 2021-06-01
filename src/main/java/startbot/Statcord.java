package startbot;

import java.text.DecimalFormat;
import java.util.Enumeration;
import net.dv8tion.jda.api.JDA;
import org.json.JSONArray;
import org.json.JSONObject;
import oshi.SystemInfo;
import java.io.IOException;
import java.net.*;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Timer;
import java.util.TimerTask;
import oshi.hardware.CentralProcessor;
import oshi.hardware.CentralProcessor.TickType;
import oshi.hardware.HardwareAbstractionLayer;
import oshi.hardware.NetworkIF;

public class Statcord {

  private static final String URL = "https://api.statcord.com/v3/stats";
  private static boolean statcordActive = false;
  private static boolean runned = false;

  private static int servers = 0;
  private static int users = 0;
  private static int commandsRun = 0;
  private static String key;
  private static String id;

  private static int memactive = 0;
  private static int memload = 0;

  private static long bandwidth;

  public static SystemInfo systemInfo = new SystemInfo(); // is public because maybe user wants some information
  private static final HardwareAbstractionLayer systemInfoHardware = systemInfo.getHardware();
  private static final CentralProcessor cpu = systemInfoHardware.getProcessor();
  private static long[] prevTicks = new long[TickType.values().length];
  private static final DecimalFormat format = new DecimalFormat("#0");

  private static String custom1 = "empty";
  private static String custom2 = "empty";

  private static JDA jda = null;
  private static JSONArray popcmd = new JSONArray();
  private static JSONArray activeuser = new JSONArray();
  private static boolean autopost = false;
  private static String NetworkName = "";
  private static long down;
  private static long up;


  private static int time = 5; // autopost timer in min
  private static int count = -1;
  public static final String ANSI_YELLOW = "\u001B[33m";
  public static final String RESET = "\033[0m";

  //TODO create start void for ShardManager or generally for sharding
  public static void start(String id, String key, JDA jda, boolean autopost, int timerInMin)
      throws Exception {
    System.out.println(ANSI_YELLOW + "Statcord started with this: "
        + id + " "
        + key + " "
        + jda.toString() + RESET);

    //save important stuff
    Statcord.jda = jda;
    Statcord.key = key;
    Statcord.id = id;
    getNetworkName();

    getNetworkSpeed();

    bandwidth = getNetworkSpeed();

    //make it active
    statcordActive = true;

    time = timerInMin;

    if (autopost) {
      autorun();
      System.out.println(ANSI_YELLOW + "!!! [Statcord] autorun activated!" + RESET);
      Statcord.autopost = true;
    }

  }

  //manually updating Stats
  public static void updateStats() throws IOException, InterruptedException {
    if (!statcordActive) {
      System.out.println(ANSI_YELLOW + "[Statcord]You can not use 'updateStats' because Statcord is not active!" + RESET);
      return;
    }
   // System.out.println(ANSI_YELLOW + "[Statcord] Updating Statcord!" + RESET);

    servers = jda.getGuilds().size();
    users = jda.getUsers().size();
    memactive = (int) Runtime.getRuntime().totalMemory();
    memload = (int) (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory());
    double mem = ((double) memload / (double) memactive) * (double) 100;
    int memperc = (int) Math.round(mem);


    JSONObject post = new JSONObject();
    post.put("id", id);
    post.put("key", key);
    post.put("servers", String.valueOf(servers));
    post.put("users", String.valueOf(users));
    post.put("active", activeuser);
    post.put("commands", String.valueOf(commandsRun));
    post.put("popular", popcmd);
    post.put("memactive", String.valueOf(memload));
    post.put("memload", String.valueOf(memperc));
    post.put("cpuload", getCPULoad());
    post.put("bandwidth", String.valueOf(bandwidth));
    if (!custom1.equalsIgnoreCase("empty")) {
      post.put("custom1", custom1);
    }
    if (!custom2.equalsIgnoreCase("empty")) {
      post.put("custom2", custom2);
    }

    String body = post.toString();

    //System.out.println(body);
    post(body);

    commandsRun = 0;
    popcmd = new JSONArray();
    activeuser = new JSONArray();
    custom2 = "empty";
    custom1 = "empty";
  }

  // command metrics with active users
  public static void commandPost(String command, String author) {
    if (!statcordActive) {
      System.out.println(ANSI_YELLOW + "[Statcord]You can not use 'commandPost' because Statcord is not active!" + RESET);
      return;
    }
   // System.out.println("Adding a command to Statcord");

    // popular cmds
    JSONObject obj = new JSONObject();
    if (!userexists(popcmd, command)) {
      obj.put("name", command);
      obj.put("count", 1);
      popcmd.put(obj);
      commandsRun++;
    } else {
      for (int i = 0; i < popcmd.length(); i++) {
        if (popcmd.getJSONObject(i).getString("name").equalsIgnoreCase(command)) {
          int test = popcmd.getJSONObject(i).getInt("count") + 1;
          popcmd.getJSONObject(i).put("count", test);
          commandsRun++;
          break;
        }
      }
    }

    //active users
    if (!activeuser.toString().contains(author)) {
      activeuser.put(author);
    }

  }

  //boolean if a value is existing in a jsonarray (for popular cmds)
  private static boolean userexists(JSONArray jsonArray, String usernameToFind) {
    return jsonArray.toString().contains("\"name\":\"" + usernameToFind + "\"");
  }

  //http post to statcord
  private static void post(String body) throws IOException, InterruptedException {
    System.out.println(body);
    HttpClient client = HttpClient.newHttpClient();
    HttpRequest request = HttpRequest.newBuilder()
        .uri(URI.create(URL))
        .POST(HttpRequest.BodyPublishers.ofString(body))
        .header("Content-Type", "application/json")
        .build();

    HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

    if (response.body().contains("error\":false")) {
     // System.out.println(ANSI_YELLOW + "[Statcord] Updated Stats on Statcord!" + RESET);
    } else {
      System.out.println("[Statcord] An error happened");
      System.out.println("Status code: " + response.statusCode());
      System.out.println("Response body: " + response.body());
    }
  }

  public static String getCPULoad() {
    double cpuLoad = Math.round((cpu.getSystemCpuLoadBetweenTicks(prevTicks) * 100) + 1.0D);
    prevTicks = cpu.getSystemCpuLoadTicks();
    return format.format(cpuLoad);
  }

  public static void getNetworkName() throws Exception {
    Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
    // hostname is passed to your method
    InetAddress myAddr = InetAddress.getLocalHost();

    while (networkInterfaces.hasMoreElements()) {
      NetworkInterface networkInterface = networkInterfaces.nextElement();
      Enumeration<InetAddress> inAddrs = networkInterface.getInetAddresses();
      while (inAddrs.hasMoreElements()) {
        InetAddress inetAddress = inAddrs.nextElement();
        if (inetAddress.equals(myAddr)) {
          NetworkName = networkInterface.getName();
          return;
        }
      }
    }
    throw new Exception("Not found network hostname");
  }

  public static long getNetworkSpeed() {
    if (!runned) {
      for (int d = 0; d < systemInfo.getHardware().getNetworkIFs().size(); d++) {
        count++;
        if (systemInfo.getHardware().getNetworkIFs().get(d).getName().equals(NetworkName)) {
          runned = true;
          break;
        }
      }
    }
    NetworkIF[] networkIFs = systemInfo.getHardware().getNetworkIFs().toArray(new NetworkIF[count]);
    int i = 0;
    NetworkIF net = networkIFs[count];
    try {
      while (!networkIFs[i].getName().equals(NetworkName)) {
        net = networkIFs[i];
        i++;
      }
    } catch (ArrayIndexOutOfBoundsException e) {
      e.printStackTrace();
    }

    long download1 = net.getBytesRecv();
    long upload1 = net.getBytesSent();
    long timestamp1 = net.getTimeStamp();
    try {
      Thread.sleep(1000);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
    net.updateAttributes(); //Updating network stats
    long download2 = net.getBytesRecv();
    long upload2 = net.getBytesSent();
    long timestamp2 = net.getTimeStamp();

    down = (download2 - download1) / (timestamp2 - timestamp1);
    up = (upload2 - upload1) / (timestamp2 - timestamp1);

    return down + up;
  }

  //autorun set to N min
  public static void autorun() {
    Timer timer = new Timer();

    timer.schedule(new TimerTask() {
      public void run() throws NullPointerException {
        System.out.println(ANSI_YELLOW + "[33m[Statcord] Automatic update!" + RESET);
        try {
          updateStats();
        } catch (IOException | InterruptedException e) {
          Thread.currentThread().interrupt();
          e.printStackTrace();
        }
      }
    }, 1000, time * 60000L);
  }
}