import * as figures from 'figures'
import { Box, BoxProps, Color } from 'ink'
import * as React from 'react'
import { KeyPressed } from './BoxPrompt'

interface Props extends BoxProps {
  label: string
  checked: boolean
  focus: boolean
  keyPressed: KeyPressed
  onChange: (value: boolean) => void
}

export const Checkbox: React.FC<Props> = props => {
  const symbol = props.checked ? figures.radioOn : figures.radioOff
  const { label, checked, focus, onChange, keyPressed, ...rest } = props

  React.useEffect(() => {
    if (focus && keyPressed.key === 'submit') {
      onChange(!checked)
    }
  }, [focus, checked, keyPressed.key, keyPressed.str])

  return (
    <Box {...rest}>
      {focus ? <Color blue>{symbol}</Color> : symbol}
      <Box marginLeft={1}>{focus ? <Color blue>{label}</Color> : label}</Box>
    </Box>
  )
}
