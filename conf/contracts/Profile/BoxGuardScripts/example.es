// NOTE: INPUTS(3) to INPUTS(6) have same unique propBytes
val slicedInputs = INPUTS.slice(3, 6).filter{ (box: Box) => box.propositionBytes == INPUTS(3).propositionBytes }

def getProof(p: Box): Coll[Byte] = {
    if(p.id != SELF.id){
      if(p.propositionBytes == INPUTS(3).propositionBytes){
        p.R4[Coll[Byte]].get
      }else{
        Coll[Byte]()
      }
    }else{
      Coll[Byte]()
    }
}
val initProof: Coll[Byte] = Coll[Byte]()

val proof: Coll[Byte]         = slicedInputs.fold(initProof, {
    (z: Coll[Byte], p: Box) =>
      z.append( getProof(p) )
  })