import {
  addProject,
  deployProject,
  deleteProject,
  getCloudClusterEnvironment,
} from './TestHelper'

describe('login and project workflow', () => {
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

  test('add project in EU', async () => {
    const { workspaceSlug, clusterToken } = await getCloudClusterEnvironment(
      CLOUD_API_ENDPOINT,
      EU_REGION,
      USER_EMAIL,
      USER_PASSWORD,
      USER_NAME,
      SERVICE_NAME,
      STAGE_NAME,
    )
    const addProjectPayload = await addProject(
      EU_CLUSTER_ENDPOINT,
      workspaceSlug,
      SERVICE_NAME,
      STAGE_NAME,
      clusterToken,
    )
    expect(addProjectPayload).toMatchSnapshot()
  })

  test('deploy project in EU', async () => {
    const { workspaceSlug, clusterToken } = await getCloudClusterEnvironment(
      CLOUD_API_ENDPOINT,
      EU_REGION,
      USER_EMAIL,
      USER_PASSWORD,
      USER_NAME,
      SERVICE_NAME,
      STAGE_NAME,
    )
    
    const deployProjectPayload = await deployProject(
      EU_CLUSTER_ENDPOINT,
      workspaceSlug,
      SERVICE_NAME,
      STAGE_NAME,
      clusterToken,
    )
    const { errors } = deployProjectPayload
    expect(errors.length).toBe(0)
  })

  test('delete project in EU', async () => {
    const { workspaceSlug, clusterToken } = await getCloudClusterEnvironment(
      CLOUD_API_ENDPOINT,
      EU_REGION,
      USER_EMAIL,
      USER_PASSWORD,
      USER_NAME,
      SERVICE_NAME,
      STAGE_NAME,
    )
    
    const deletedProjectPayload = await deleteProject(
      EU_CLUSTER_ENDPOINT,
      workspaceSlug,
      SERVICE_NAME,
      STAGE_NAME,
      clusterToken,
    )
    expect(deletedProjectPayload).toMatchSnapshot()
  })

  test('add project in US', async () => {
    const { workspaceSlug, clusterToken } = await getCloudClusterEnvironment(
      CLOUD_API_ENDPOINT,
      US_REGION,
      USER_EMAIL,
      USER_PASSWORD,
      USER_NAME,
      SERVICE_NAME,
      STAGE_NAME,
    )
    const addProjectPayload = await addProject(
      US_CLUSTER_ENDPOINT,
      workspaceSlug,
      SERVICE_NAME,
      STAGE_NAME,
      clusterToken,
    )
    expect(addProjectPayload).toMatchSnapshot()
  })

  test('deploy project in US', async () => {
    const { workspaceSlug, clusterToken } = await getCloudClusterEnvironment(
      CLOUD_API_ENDPOINT,
      US_REGION,
      USER_EMAIL,
      USER_PASSWORD,
      USER_NAME,
      SERVICE_NAME,
      STAGE_NAME,
    )
    
    const deployProjectPayload = await deployProject(
      US_CLUSTER_ENDPOINT,
      workspaceSlug,
      SERVICE_NAME,
      STAGE_NAME,
      clusterToken,
    )
    const { errors } = deployProjectPayload
    expect(errors.length).toBe(0)
  })

  test('delete project in US', async () => {
    const { workspaceSlug, clusterToken } = await getCloudClusterEnvironment(
      CLOUD_API_ENDPOINT,
      US_REGION,
      USER_EMAIL,
      USER_PASSWORD,
      USER_NAME,
      SERVICE_NAME,
      STAGE_NAME,
    )
    
    const deletedProjectPayload = await deleteProject(
      US_CLUSTER_ENDPOINT,
      workspaceSlug,
      SERVICE_NAME,
      STAGE_NAME,
      clusterToken,
    )
    expect(deletedProjectPayload).toMatchSnapshot()
  })
})
