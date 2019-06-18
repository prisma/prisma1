import chalk from 'chalk'

export interface Template {
  name: string
  language: 'javascript' | 'typescript'
  description: string
  repo: TemplateRepository
  postIntallMessage: string
}

export interface TemplateRepository {
  uri: string
  branch: string
  path: string
}

export const defaultTemplate: Template = {
  name: 'graphql_boilerplate',
  language: 'typescript',
  description: 'GraphQL starter with Prisma 2',
  repo: {
    uri: 'https://github.com/prisma/photonjs',
    branch: 'master',
    path: '/examples/typescript/graphql',
  },
  postIntallMessage: `
Your template has been successfully set up!
  
Here are the next steps to get you started:
  1. Run ${chalk.yellow(`yarn seed`)} to seed the database. 
  2. Run ${chalk.yellow(`yarn start`)} (Starts the GraphQL server)
  3. That's it !
  `,
}

export const availableTemplates: Template[] = [
  defaultTemplate,
  {
    name: 'rest_boilerplate',
    language: 'typescript',
    description: 'REST with express server starter with Prisma 2',
    repo: {
      uri: 'https://github.com/prisma/photonjs',
      branch: 'master',
      path: '/examples/typescript/rest-express',
    },
    postIntallMessage: `
Your template has been successfully set up!
  
Here are the next steps to get you started:
  1. Run ${chalk.yellow(`yarn seed`)} to seed the database. 
  2. Run ${chalk.yellow(`yarn start`)} (Starts the REST with express server)
  3. That's it !
  `,
  },
  {
    name: 'grpc_boilerplate',
    language: 'typescript',
    description: 'REST with express server starter with Prisma 2',
    repo: {
      uri: 'https://github.com/prisma/photonjs',
      branch: 'master',
      path: '/examples/typescript/grpc',
    },
    postIntallMessage: `
Your template has been successfully set up!
  
Here are the next steps to get you started:
  1. Run ${chalk.yellow(`yarn seed`)} to seed the database. 
  2. Run ${chalk.yellow(`yarn start`)} (Starts the gRPC server)
  3. That's it !
  `,
  },
  {
    name: 'graphql_boilerplate',
    language: 'javascript',
    description: 'GraphQL starter with Prisma 2',
    repo: {
      uri: 'https://github.com/prisma/photonjs',
      branch: 'master',
      path: '/examples/javascript/graphql',
    },
    postIntallMessage: `
  Your template has been successfully set up!
    
  Here are the next steps to get you started:
    1. Run ${chalk.yellow(`yarn seed`)} to seed the database. 
    2. Run ${chalk.yellow(`yarn start`)} (Starts the GraphQL server)
    3. That's it !
    `,
  },
  {
    name: 'rest_boilerplate',
    language: 'javascript',
    description: 'REST with express server starter with Prisma 2',
    repo: {
      uri: 'https://github.com/prisma/photonjs',
      branch: 'master',
      path: '/examples/javascript/rest-express',
    },
    postIntallMessage: `
Your template has been successfully set up!
  
Here are the next steps to get you started:
  1. Run ${chalk.yellow(`yarn seed`)} to seed the database. 
  2. Run ${chalk.yellow(`yarn start`)} (Starts the REST with express server)
  3. That's it !
  `,
  },
  {
    name: 'grpc_boilerplate',
    language: 'javascript',
    description: 'REST with express server starter with Prisma 2',
    repo: {
      uri: 'https://github.com/prisma/photonjs',
      branch: 'master',
      path: '/examples/javascript/grpc',
    },
    postIntallMessage: `
Your template has been successfully set up!
  
Here are the next steps to get you started:
  1. Run ${chalk.yellow(`yarn seed`)} to seed the database. 
  2. Run ${chalk.yellow(`yarn start`)} (Starts the gRPC server)
  3. That's it !
  `,
  },
]

export const templatesNames = availableTemplates.map(t => `\`${t.name}\``).join(', ')

export function findTemplate(name, language) {
  return availableTemplates.find(template => template.name === name && template.language === language.toLowerCase())
}
