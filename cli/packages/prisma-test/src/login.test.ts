import {
  getUserToken,
  getWorkspaceSlug,
  getClusterToken,
  addProject,
  deployProject,
  deleteProject,
} from './TestHelper'

describe('login and add project', () => {
  const CLOUD_API_ENDPOINT = 'https://api.cloud.prisma.sh/'
  const EU_CLUSTER_ENDPOINT = 'https://eu1.prisma.sh/management'
  const EU_REGION = 'prisma-eu1'
  const US_CLUSTER_ENDPOINT = 'https://us1.prisma.sh/management'
  const US_REGION = 'prisma-us1'
  const USER_EMAIL = 'test@test.com'
  const USER_PASSWORD = 'test'
  const USER_NAME = 'Test'
  const SERVICE_NAME = 'test'
  const STAGE_NAME = 'test'

  test('in EU', async () => {
    const userToken = await getUserToken(
      CLOUD_API_ENDPOINT,
      USER_EMAIL,
      USER_PASSWORD,
      USER_NAME,
    )
    const workspaceSlug = await getWorkspaceSlug(CLOUD_API_ENDPOINT, userToken)
    const clusterToken = await getClusterToken(
      CLOUD_API_ENDPOINT,
      workspaceSlug,
      EU_REGION,
      SERVICE_NAME,
      STAGE_NAME,
      userToken,
    )
    const projectPayload = await addProject(
      EU_CLUSTER_ENDPOINT,
      workspaceSlug,
      SERVICE_NAME,
      STAGE_NAME,
      clusterToken,
    )
    const projectName = (projectPayload as any).project.name
    expect(projectName).toBe(`${workspaceSlug}~${SERVICE_NAME}`)

    const deployProjectPayload = await deployProject(
      EU_CLUSTER_ENDPOINT,
      workspaceSlug,
      SERVICE_NAME,
      STAGE_NAME,
      clusterToken,
    )
    const { errors } = deployProjectPayload
    expect(errors.length).toBe(0)

    const deletedProjectPayload = await deleteProject(
      EU_CLUSTER_ENDPOINT,
      workspaceSlug,
      SERVICE_NAME,
      STAGE_NAME,
      clusterToken,
    )
    const deletedProjectName = (deletedProjectPayload as any).project.name
    expect(projectName).toBe(deletedProjectName)
  })

  test('in US', async () => {
    const userToken = await getUserToken(
      CLOUD_API_ENDPOINT,
      USER_EMAIL,
      USER_PASSWORD,
      USER_NAME,
    )
    const workspaceSlug = await getWorkspaceSlug(CLOUD_API_ENDPOINT, userToken)
    const clusterToken = await getClusterToken(
      CLOUD_API_ENDPOINT,
      workspaceSlug,
      US_REGION,
      SERVICE_NAME,
      STAGE_NAME,
      userToken,
    )
    const projectPayload = await addProject(
      US_CLUSTER_ENDPOINT,
      workspaceSlug,
      SERVICE_NAME,
      STAGE_NAME,
      clusterToken,
    )
    const projectName = (projectPayload as any).project.name
    expect(projectName).toBe(`${workspaceSlug}~${SERVICE_NAME}`)

    const deployProjectPayload = await deployProject(
      US_CLUSTER_ENDPOINT,
      workspaceSlug,
      SERVICE_NAME,
      STAGE_NAME,
      clusterToken,
    )
    const { errors } = deployProjectPayload
    expect(errors.length).toBe(0)

    const deletedProjectPayload = await deleteProject(
      US_CLUSTER_ENDPOINT,
      workspaceSlug,
      SERVICE_NAME,
      STAGE_NAME,
      clusterToken,
    )
    const deletedProjectName = (deletedProjectPayload as any).project.name
    expect(projectName).toBe(deletedProjectName)
  })
})
