export default interface IDatabaseClient {
  query(query: string, variables: any[]): Promise<any[]> 
}