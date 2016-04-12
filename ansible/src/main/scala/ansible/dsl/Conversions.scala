package ansible.dsl

import ansible.CommonOptions.Tag

trait Conversions {
  implicit def str2tag(s: String): Tag = Tag(s)
}
