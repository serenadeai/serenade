package offline;

import net.sourceforge.argparse4j.inf.Namespace;
import net.sourceforge.argparse4j.inf.Subparsers;

public interface Subcommand {
  void configureSubparsers(Subparsers subparsers);
  void run(Namespace namespace);
}
