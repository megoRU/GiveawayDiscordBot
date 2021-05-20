package threads;

public class Giveaway {

  private final Long ID_GUILD;
  private final String TIME;

  public Giveaway(Long id_guild, String time) {
    ID_GUILD = id_guild;
    TIME = time;
  }

  public Long getID_GUILD() {
    return ID_GUILD;
  }

  public String getTIME() {
    return TIME;
  }

}
