export interface User {
  id: string
  name: string
  login: Login[]
}

export interface Login {
  email: string
}
