import chalk from 'chalk'

export class CommandRemovedError extends Error {
  constructor(
    versionWhenDeprecated: string,
    releaseNotesLink: string,
    additionalInfo?: string,
  ) {
    super(
      `As mentioned in Prisma ${versionWhenDeprecated} release notes: ${releaseNotesLink}
      this command was deprecated and is now removed. ${additionalInfo}`,
    )
  }
}
