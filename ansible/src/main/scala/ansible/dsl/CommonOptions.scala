package ansible.dsl

import ansible.CommonOptions._

trait CommonOptions {
  implicit class CommonOptionsSyntax[T, O](t: T)(implicit o: Optics[T, O]) {
    def tags = o.tags.getOption(t)

    def env = o.env.getOption(t)

    def serial = o.serial.getOption(t)

    def remoteUser = o.remoteUser.getOption(t)

    def become = o.become.getOption(t)

    def withTags(ts: Tag*) = o.tags.set(ts.toSet)(t)

    def addingTags(ts: Tag*) =
      if (o.tags.isMatching(t)) o.tags.modify(_ ++ ts.toSet)(t)
      else withTags(ts: _*)

    def withEnv(e: Map[String, String]) = o.env.set(e)(t)

    def mergeEnv(e: Map[String, String]) = o.env.modify(_ ++ e)(t)

    def withSerial(s: Serial) = o.serial.set(s)(t)

    def withRemoteUser(u: String) = o.remoteUser.set(u)(t)

    def usingSudo = o.become.set(Become(None, Some(Sudo)))(t)

    def becoming(b: Become): T = o.become.set(b)(t)

    def becoming(user: String, m: BecomeMethod = Sudo): T =
      o.become.set(Become(Some(user), Some(m)))(t)
  }

  implicit def str2tag(s: String): Tag = Tag(s)
}
