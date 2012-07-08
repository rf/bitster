package libbitster;

public class Manager extends Actor {
  
  private TorrentInfo metainfo;
  private Deputy deputy;
  
  public Manager(TorrentInfo metainfo)
  {
    super();
    this.metainfo = metainfo;
    deputy = new Deputy();
    deputy.post(new Memo("url", metainfo.announce_url));
  }
 
}
