import * as promisify from 'util.promisify'
import * as forge from 'node-forge'

export async function createRsaKeyPair(): Promise<KeyPair> {
  const keypair = await promisify(forge.pki.rsa.generateKeyPair)({
    bits: 2048,
  })

  return {
    public: forge.pki.publicKeyToPem(keypair.publicKey),
    private: forge.pki.privateKeyToPem(keypair.privateKey),
  }
}

export interface KeyPair {
  public: string
  private: string
}
