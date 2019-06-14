import { Dictionary, GeneratorDefinitionWithPackage } from '@prisma/cli'
import { generatorDefinition as photonDefinition } from '@prisma/photon'
import { generatorDefinition as photogenDefinition } from 'photogen'

const photon: GeneratorDefinitionWithPackage = {
  packagePath: '@prisma/photon',
  definition: photonDefinition,
}

const photogen: GeneratorDefinitionWithPackage = {
  packagePath: 'photogen',
  definition: photogenDefinition,
}

export const predefinedGenerators: Dictionary<GeneratorDefinitionWithPackage> = {
  photon: photon,
  javascript: photon,
  typescript: photon,
  photogen,
}
