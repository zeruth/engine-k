package rs.engine.entity

class Obj(level: Int, x: Int, z: Int, lifeCycle: Int,
          val type: Int,
          val count: Int) : Entity(level, x, z, 1, 1, lifeCycle)