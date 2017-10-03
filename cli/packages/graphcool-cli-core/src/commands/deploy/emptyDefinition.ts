import { ProjectDefinition } from 'graphcool-cli-engine'

export const emptyDefinition: ProjectDefinition = {
  modules: [
    {
      name: '',
      content: `\
types: ./types.graphql
`,
      files: {
        './types.graphql': ``,
      },
    },
  ],
}
