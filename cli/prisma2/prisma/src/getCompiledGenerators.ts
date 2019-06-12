import { CompiledGeneratorDefinition, GeneratorDefinition, Dictionary } from '@prisma/cli'
import { getConfig } from './getConfig'
import { generatorDefinition as photon } from '@prisma/photon'
import { generatorDefinition as photogen } from 'photogen'
import path from 'path'

const predefinedGenerators: Dictionary<GeneratorDefinition> = {
  photon: photon,
  javascript: photon,
  typescript: photon,
  photogen,
}

export async function getCompiledGenerators(cwd: string): Promise<CompiledGeneratorDefinition[]> {
  const config = await getConfig(cwd)
  const generators = config.generators.map(g => ({
    ...g,
    output: g.output && !g.output.startsWith('/') ? path.join(cwd, g.output) : null,
  }))

  generators.sort((a, b) => (a.provider === 'photogen' ? 1 : b.provider === 'photogen' ? -1 : a.name < b.name ? -1 : 1))

  return generators.map((definition, index) => {
    const generator = predefinedGenerators[definition.provider]
    if (!generator) {
      throw new Error(
        `Unknown generator provider ${definition.provider} for generator ${definition.name} defined in ${path.join(
          cwd,
          'datamodel.prisma',
        )}`,
      )
    }

    const otherGenerators = generators.filter((g, i) => i !== index)

    return {
      prettyName: generator.prettyName,
      generate: () =>
        generator.generate({
          cwd,
          generator: definition,
          otherGenerators,
        }),
    } as CompiledGeneratorDefinition
  })
}
