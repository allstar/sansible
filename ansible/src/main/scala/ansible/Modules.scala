package ansible

import argonaut.Json

private [ansible] case class ModuleCall(json: Json)

trait Module {
  def call: ModuleCall
}

object Modules {
  @expand trait Modules
}
