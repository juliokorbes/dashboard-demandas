import {
  useCallback,
  useEffect,
  useMemo,
  useRef,
  useState,
} from 'react'

import {
  BarChart,
  Bar,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
  PieChart,
  Pie,
  Cell,
} from 'recharts'

import './App.css'

const QUALIFICATION_COLORS = [
  '#ff6577',
  '#ff9f43',
  '#ffc95c',
  '#43d8a0',
  '#3ca9ff',
  '#a7b7ca',
]

const tooltipStyle = {
  backgroundColor: '#081a35',
  border: '1px solid rgba(255,255,255,0.08)',
  borderRadius: '10px',
  color: '#ffffff',
  boxShadow: '0 10px 30px rgba(0,0,0,0.35)',
}

const mappingFields = [
  {
    key: 'externalId',
    label: 'Código',
    aliases: [
      'codigo',
      'código',
      'id',
      'identificador',
    ],
  },
  {
    key: 'protocolOnr',
    label: 'Protocolo ONR',
    aliases: [
      'protocolo onr',
      'protocolo',
      'onr',
    ],
  },
  {
    key: 'type',
    label: 'Serviço',
    aliases: [
      'servico',
      'serviço',
      'tipo',
      'tipo de servico',
      'tipo de serviço',
    ],
  },
  {
    key: 'stage',
    label: 'Etapa',
    aliases: [
      'etapa',
      'fase',
    ],
  },
  {
    key: 'responsible',
    label: 'Responsável atual',
    aliases: [
      'responsavel atual',
      'responsável atual',
      'responsavel',
      'responsável',
    ],
  },
  {
    key: 'status',
    label: 'Status',
    aliases: [
      'status',
      'situacao',
      'situação',
    ],
  },
  {
    key: 'entryDate',
    label: 'Cadastro',
    aliases: [
      'cadastro',
      'data cadastro',
      'data de cadastro',
    ],
  },
  {
    key: 'qualificationDate',
    label: 'Qualificação',
    aliases: [
      'qualificacao',
      'qualificação',
      'data qualificacao',
      'data de qualificacao',
      'data de qualificação',
    ],
  },
  {
    key: 'deadline',
    label: 'Vencimento',
    aliases: [
      'vencimento',
      'data vencimento',
      'data de vencimento',
      'prazo',
    ],
  },
  {
    key: 'reentryDate',
    label: 'Reingresso',
    aliases: [
      'reingresso',
      'data reingresso',
      'data de reingresso',
    ],
  },
]

function normalizeText(value) {
  if (!value) {
    return ''
  }

  return value
      .normalize('NFD')
      .replace(/[\u0300-\u036f]/g, '')
      .toLowerCase()
      .replace(/[._\-\/]/g, ' ')
      .replace(/\s+/g, ' ')
      .trim()
}

function isCompatibleHeader(field, header) {
  const normalizedHeader =
      normalizeText(header)

  return field.aliases.some((alias) => {
    const normalizedAlias =
        normalizeText(alias)

    if (
        normalizedHeader ===
        normalizedAlias
    ) {
      return true
    }

    const headerWords =
        normalizedHeader.split(' ')

    const aliasWords =
        normalizedAlias.split(' ')

    if (aliasWords.length === 1) {
      return headerWords.includes(
          normalizedAlias
      )
    }

    return normalizedHeader.includes(
        normalizedAlias
    )
  })
}

function getAvailableHeaders(
    field,
    headers
) {
  return headers.filter((header) => {
    return isCompatibleHeader(
        field,
        header
    )
  })
}

function createAutomaticMapping(headers) {
  const result = {}
  const usedHeaders = new Set()

  mappingFields.forEach((field) => {
    const matchingHeader =
        headers.find((header) => {
          return (
              !usedHeaders.has(header) &&
              isCompatibleHeader(
                  field,
                  header
              )
          )
        })

    if (matchingHeader) {
      result[field.key] =
          matchingHeader

      usedHeaders.add(
          matchingHeader
      )
    } else {
      result[field.key] = ''
    }
  })

  return result
}

function formatCategory(value) {
  if (!value) {
    return '-'
  }

  return value
      .replaceAll('_', ' ')
      .toLowerCase()
      .split(' ')
      .map((word) => {
        if (!word) {
          return word
        }

        return (
            word.charAt(0).toUpperCase() +
            word.slice(1)
        )
      })
      .join(' ')
}

function formatDate(date) {
  if (!date) {
    return '-'
  }

  const [year, month, day] =
      date.split('-')

  return `${day}/${month}/${year}`
}

function formatDateTime(value) {
  if (!value) {
    return '-'
  }

  return new Date(value)
      .toLocaleString('pt-BR')
}

function getTodayInputDate() {
  const today = new Date()

  const year =
      today.getFullYear()

  const month =
      String(
          today.getMonth() + 1
      ).padStart(2, '0')

  const day =
      String(
          today.getDate()
      ).padStart(2, '0')

  return `${year}-${month}-${day}`
}

function getNowInputDateTime() {
  const now = new Date()

  const year =
      now.getFullYear()

  const month =
      String(
          now.getMonth() + 1
      ).padStart(2, '0')

  const day =
      String(
          now.getDate()
      ).padStart(2, '0')

  const hours =
      String(
          now.getHours()
      ).padStart(2, '0')

  const minutes =
      String(
          now.getMinutes()
      ).padStart(2, '0')

  return `${year}-${month}-${day}T${hours}:${minutes}`
}

function getDateFromDateTime(value) {
  if (!value) {
    return getTodayInputDate()
  }

  return value.slice(0, 10)
}

function formatSnapshotDateTime(value) {
  if (!value) {
    return '-'
  }

  const [
    date,
    time = '',
  ] = value.split('T')

  const formattedTime =
      time.slice(0, 5)

  return `${formatDate(date)} - ${formattedTime}`
}

function isCompleted(status) {
  return (
      normalizeText(status) ===
      'concluida'
  )
}

function calculateSituation(
    demand,
    referenceDate = getTodayInputDate()
) {
  if (isCompleted(demand.status)) {
    return 'completed'
  }

  if (!demand.qualificationDate) {
    return 'no-deadline'
  }

  const comparisonDate =
      new Date(
          `${referenceDate}T00:00:00`
      )

  const qualificationDate =
      new Date(
          `${demand.qualificationDate}T00:00:00`
      )

  const difference =
      Math.round(
          (qualificationDate - comparisonDate) /
          (
              1000 *
              60 *
              60 *
              24
          )
      )

  if (difference < 0) {
    return 'overdue'
  }

  if (difference === 0) {
    return 'today'
  }

  if (difference <= 5) {
    return 'due-soon'
  }

  return 'on-time'
}

function buildHistoricalDashboard(
    demands,
    referenceDate
) {
  const summary = {
    total: demands.length,
    overdue: 0,
    today: 0,
    dueSoon: 0,
    onTime: 0,
    completed: 0,
    noDeadline: 0,
  }

  const delayCounts = {
    '1 a 5 dias': 0,
    '6 a 10 dias': 0,
    '11 a 30 dias': 0,
    'Mais de 30 dias': 0,
  }

  const sectorCounts = new Map()
  const typeCounts = new Map()
  const critical = []

  const comparisonDate = new Date(
      `${referenceDate}T00:00:00`
  )

  demands.forEach((demand) => {
    const situation =
        calculateSituation(
            demand,
            referenceDate
        )

    if (situation === 'overdue') {
      summary.overdue += 1
    } else if (situation === 'today') {
      summary.today += 1
    } else if (situation === 'due-soon') {
      summary.dueSoon += 1
    } else if (situation === 'on-time') {
      summary.onTime += 1
    } else if (situation === 'completed') {
      summary.completed += 1
    } else if (situation === 'no-deadline') {
      summary.noDeadline += 1
    }

    const sector =
        demand.sector ||
        'NAO_INFORMADO'

    sectorCounts.set(
        sector,
        (sectorCounts.get(sector) || 0) + 1
    )

    const type =
        demand.type ||
        'NÃO INFORMADO'

    typeCounts.set(
        type,
        (typeCounts.get(type) || 0) + 1
    )

    if (
        !isCompleted(demand.status) &&
        demand.qualificationDate
    ) {
      const qualificationDate =
          new Date(
              `${demand.qualificationDate}T00:00:00`
          )

      const daysOverdue = Math.round(
          (comparisonDate - qualificationDate) /
          (1000 * 60 * 60 * 24)
      )

      if (daysOverdue >= 0) {
        critical.push({
          id: demand.id,
          externalId:
          demand.externalId,
          type: demand.type,
          sector: demand.sector,
          deadline:
          demand.qualificationDate,
          daysOverdue,
          status: demand.status,
        })
      }

      if (daysOverdue > 0) {
        if (daysOverdue <= 5) {
          delayCounts['1 a 5 dias'] += 1
        } else if (daysOverdue <= 10) {
          delayCounts['6 a 10 dias'] += 1
        } else if (daysOverdue <= 30) {
          delayCounts['11 a 30 dias'] += 1
        } else {
          delayCounts['Mais de 30 dias'] += 1
        }
      }
    }
  })

  critical.sort((a, b) => {
    return (
        a.deadline || ''
    ).localeCompare(
        b.deadline || ''
    )
  })

  const delayRanges =
      Object.entries(delayCounts)
          .map(([range, count]) => ({
            range,
            count,
          }))

  const sectorDistribution =
      [...sectorCounts.entries()]
          .map(([category, count]) => ({
            category,
            count,
          }))
          .sort((a, b) =>
              b.count - a.count
          )

  const typeDistribution =
      [...typeCounts.entries()]
          .map(([category, count]) => ({
            category,
            count,
          }))
          .sort((a, b) =>
              b.count - a.count
          )

  return {
    summary,
    critical,
    delayRanges,
    sectorDistribution,
    typeDistribution,
  }
}

function situationLabel(situation) {
  const labels = {
    overdue: 'Atrasada',
    today: 'Qualifica hoje',
    'due-soon':
        'Qualificação próxima',
    'on-time':
        'Qualificação futura',
    completed:
        'Concluída',
    'no-deadline':
        'Sem qualificação',
  }

  return (
      labels[situation] ?? '-'
  )
}

function App() {
  const fileInputRef =
      useRef(null)

  const [
    summary,
    setSummary,
  ] = useState(null)

  const [
    allDemands,
    setAllDemands,
  ] = useState([])

  const [
    criticalDemands,
    setCriticalDemands,
  ] = useState([])

  const [
    delayRanges,
    setDelayRanges,
  ] = useState([])

  const [
    sectorDistribution,
    setSectorDistribution,
  ] = useState([])

  const [
    typeDistribution,
    setTypeDistribution,
  ] = useState([])

  const [
    lastImport,
    setLastImport,
  ] = useState(null)

  const [
    loading,
    setLoading,
  ] = useState(true)

  const [
    search,
    setSearch,
  ] = useState('')

  const [
    typeFilter,
    setTypeFilter,
  ] = useState('')

  const [
    sectorFilter,
    setSectorFilter,
  ] = useState('')

  const [
    statusFilter,
    setStatusFilter,
  ] = useState('')

  const [
    situationFilter,
    setSituationFilter,
  ] = useState('')

  const [
    importOpen,
    setImportOpen,
  ] = useState(false)

  const [
    selectedFile,
    setSelectedFile,
  ] = useState(null)

  const [
    preview,
    setPreview,
  ] = useState(null)

  const [
    mapping,
    setMapping,
  ] = useState({})

  const [
    importing,
    setImporting,
  ] = useState(false)

  const [
    importError,
    setImportError,
  ] = useState('')

  const [
    importResult,
    setImportResult,
  ] = useState(null)

  const [
    referenceDateTime,
    setReferenceDateTime,
  ] = useState(
      getNowInputDateTime
  )

  const [
    historyDateTimes,
    setHistoryDateTimes,
  ] = useState([])

  const [
    selectedViewDateTime,
    setSelectedViewDateTime,
  ] = useState('')

  const [
    viewLoading,
    setViewLoading,
  ] = useState(false)

  const loadDashboard =
      useCallback(async () => {
        try {
          const responses =
              await Promise.all([
                fetch(
                    '/dashboard/summary'
                ),
                fetch(
                    '/dashboard/critical'
                ),
                fetch(
                    '/dashboard/delay-ranges'
                ),
                fetch(
                    '/dashboard/sector-distribution'
                ),
                fetch(
                    '/dashboard/type-distribution'
                ),
                fetch(
                    '/demands'
                ),
                fetch(
                    '/import/history/latest'
                ),
                fetch(
                    '/history/dates'
                ),
              ])

          const [
            summaryResponse,
            criticalResponse,
            delayResponse,
            sectorResponse,
            typeResponse,
            demandsResponse,
            historyResponse,
            historyDateTimesResponse,
          ] = responses

          const requiredResponses = [
            summaryResponse,
            criticalResponse,
            delayResponse,
            sectorResponse,
            typeResponse,
            demandsResponse,
          ]

          if (
              requiredResponses.some(
                  (response) =>
                      !response.ok
              )
          ) {
            throw new Error(
                'Erro ao buscar os dados da dashboard.'
            )
          }

          const [
            summaryData,
            criticalData,
            delayData,
            sectorData,
            typeData,
            demandsData,
          ] =
              await Promise.all([
                summaryResponse.json(),
                criticalResponse.json(),
                delayResponse.json(),
                sectorResponse.json(),
                typeResponse.json(),
                demandsResponse.json(),
              ])

          setSummary(summaryData)

          setCriticalDemands(
              criticalData
          )

          setDelayRanges(
              delayData
          )

          setSectorDistribution(
              sectorData
          )

          setTypeDistribution(
              typeData
          )

          setAllDemands(
              demandsData
          )

          if (
              historyResponse.status ===
              204
          ) {
            setLastImport(null)
          } else if (
              historyResponse.ok
          ) {
            const historyData =
                await historyResponse.json()

            setLastImport(
                historyData
            )
          } else {
            setLastImport(null)
          }

          if (historyDateTimesResponse.ok) {
            const dates =
                await historyDateTimesResponse.json()

            setHistoryDateTimes(dates)
          } else {
            setHistoryDateTimes([])
          }

        } catch (error) {
          console.error(
              'Erro ao carregar dashboard:',
              error
          )
        } finally {
          setLoading(false)
        }
      }, [])

  useEffect(() => {
    loadDashboard()
  }, [loadDashboard])

  async function loadHistoricalSnapshot(
      dateTime
  ) {
    setViewLoading(true)

    try {
      const response = await fetch(
          `/history/${encodeURIComponent(dateTime)}`
      )

      if (!response.ok) {
        throw new Error(
            'Não foi possível carregar o histórico selecionado.'
        )
      }

      const demands =
          await response.json()

      const historicalDashboard =
          buildHistoricalDashboard(
              demands,
              getDateFromDateTime(
                  dateTime
              )
          )

      setAllDemands(demands)
      setSummary(
          historicalDashboard.summary
      )
      setCriticalDemands(
          historicalDashboard.critical
      )
      setDelayRanges(
          historicalDashboard.delayRanges
      )
      setSectorDistribution(
          historicalDashboard.sectorDistribution
      )
      setTypeDistribution(
          historicalDashboard.typeDistribution
      )

      clearFilters()
    } catch (error) {
      console.error(
          'Erro ao carregar histórico:',
          error
      )

      alert(error.message)
    } finally {
      setViewLoading(false)
    }
  }

  async function handleViewDateTimeChange(
      event
  ) {
    const dateTime =
        event.target.value

    setSelectedViewDateTime(
        dateTime
    )

    if (!dateTime) {
      setViewLoading(true)

      try {
        clearFilters()
        await loadDashboard()
      } finally {
        setViewLoading(false)
      }

      return
    }

    await loadHistoricalSnapshot(
        dateTime
    )
  }

  const qualificationDistribution =
      useMemo(() => {
        if (!summary) {
          return []
        }

        return [
          {
            category:
                'Qualificações vencidas',
            count:
                summary.overdue ?? 0,
          },
          {
            category:
                'Qualifica hoje',
            count:
                summary.today ?? 0,
          },
          {
            category:
                'Qualificações próximas',
            count:
                summary.dueSoon ?? 0,
          },
          {
            category:
                'Qualificações futuras',
            count:
                summary.onTime ?? 0,
          },
          {
            category:
                'Concluídas',
            count:
                summary.completed ?? 0,
          },
          {
            category:
                'Sem qualificação',
            count:
                summary.noDeadline ?? 0,
          },
        ]
      }, [summary])

  const availableTypes =
      useMemo(() => {
        return [
          ...new Set(
              allDemands
                  .map(
                      (demand) =>
                          demand.type
                  )
                  .filter(Boolean)
          ),
        ].sort()
      }, [allDemands])

  const availableSectors =
      useMemo(() => {
        return [
          ...new Set(
              allDemands
                  .map(
                      (demand) =>
                          demand.sector
                  )
                  .filter(Boolean)
          ),
        ].sort()
      }, [allDemands])

  const availableStatuses =
      useMemo(() => {
        return [
          ...new Set(
              allDemands
                  .map(
                      (demand) =>
                          demand.status
                  )
                  .filter(Boolean)
          ),
        ].sort()
      }, [allDemands])

  const filteredDemands =
      useMemo(() => {
        const normalizedSearch =
            normalizeText(search)

        return allDemands
            .filter((demand) => {

              if (
                  normalizedSearch &&
                  !normalizeText(
                      demand.externalId
                  ).includes(
                      normalizedSearch
                  )
              ) {
                return false
              }

              if (
                  typeFilter &&
                  demand.type !==
                  typeFilter
              ) {
                return false
              }

              if (
                  sectorFilter &&
                  demand.sector !==
                  sectorFilter
              ) {
                return false
              }

              if (
                  statusFilter &&
                  demand.status !==
                  statusFilter
              ) {
                return false
              }

              if (
                  situationFilter &&
                  calculateSituation(
                      demand,
                      selectedViewDateTime
                          ? getDateFromDateTime(
                              selectedViewDateTime
                          )
                          : getTodayInputDate()
                  ) !== situationFilter
              ) {
                return false
              }

              return true
            })
            .sort((a, b) => {

              if (
                  !a.qualificationDate &&
                  !b.qualificationDate
              ) {
                return 0
              }

              if (!a.qualificationDate) {
                return 1
              }

              if (!b.qualificationDate) {
                return -1
              }

              return (
                  a.qualificationDate.localeCompare(
                      b.qualificationDate
                  )
              )
            })

      }, [
        allDemands,
        search,
        typeFilter,
        sectorFilter,
        statusFilter,
        situationFilter,
        selectedViewDateTime,
      ])

  function clearFilters() {
    setSearch('')
    setTypeFilter('')
    setSectorFilter('')
    setStatusFilter('')
    setSituationFilter('')
  }

  function openFileSelector() {
    fileInputRef
        .current
        ?.click()
  }

  function exportExcel() {
    if (selectedViewDateTime) {
      alert(
          'A exportação do histórico será adicionada no próximo passo. Volte para Situação atual para exportar por enquanto.'
      )

      return
    }

    window.location.href =
        '/export/excel'
  }

  async function handleFileSelected(
      event
  ) {
    const file =
        event.target.files?.[0]

    if (!file) {
      return
    }

    setSelectedFile(file)
    setPreview(null)
    setImportResult(null)
    setImportError('')
    setImportOpen(true)

    try {
      const formData =
          new FormData()

      formData.append(
          'file',
          file
      )

      const response =
          await fetch(
              '/import/preview',
              {
                method: 'POST',
                body: formData,
              }
          )

      if (!response.ok) {
        throw new Error(
            'Não foi possível ler o arquivo.'
        )
      }

      const data =
          await response.json()

      setPreview(data)

      setMapping(
          createAutomaticMapping(
              data.headers
          )
      )

    } catch (error) {
      setImportError(
          error.message
      )
    }
  }

  function changeMapping(
      field,
      value
  ) {
    setMapping(
        (current) => ({
          ...current,
          [field]: value,
        })
    )
  }

  async function confirmImport() {
    if (!selectedFile) {
      return
    }

    if (!mapping.externalId) {
      setImportError(
          'Selecione a coluna CÓDIGO.'
      )

      return
    }

    if (!referenceDateTime) {
      setImportError(
          'Selecione a data e hora da situação.'
      )

      return
    }

    setImporting(true)
    setImportError('')
    setImportResult(null)

    try {
      const formData =
          new FormData()

      formData.append(
          'file',
          selectedFile
      )

      formData.append(
          'referenceDateTime',
          referenceDateTime
      )

      Object.entries(
          mapping
      ).forEach(
          ([key, value]) => {
            if (value) {
              formData.append(
                  key,
                  value
              )
            }
          }
      )

      const response =
          await fetch(
              '/import/demands',
              {
                method: 'POST',
                body: formData,
              }
          )

      if (!response.ok) {
        throw new Error(
            'Erro ao importar o arquivo.'
        )
      }

      const result =
          await response.json()

      setImportResult(result)
      setSelectedViewDateTime('')

      await loadDashboard()

    } catch (error) {
      setImportError(
          error.message
      )
    } finally {
      setImporting(false)
    }
  }

  function closeImport() {
    setImportOpen(false)
    setSelectedFile(null)
    setPreview(null)
    setMapping({})
    setImportResult(null)
    setImportError('')
    setReferenceDateTime(
        getNowInputDateTime()
    )

    if (
        fileInputRef.current
    ) {
      fileInputRef.current.value = ''
    }
  }

  async function clearDashboard() {
    const confirmed =
        window.confirm(
            'Tem certeza que deseja apagar todas as demandas e o histórico de importações?'
        )

    if (!confirmed) {
      return
    }

    try {
      const response =
          await fetch(
              '/dashboard/clear',
              {
                method: 'DELETE',
              }
          )

      if (!response.ok) {
        throw new Error(
            'Não foi possível limpar a dashboard.'
        )
      }

      clearFilters()
      setSelectedViewDateTime('')

      await loadDashboard()

    } catch (error) {
      console.error(
          'Erro ao limpar dashboard:',
          error
      )

      alert(
          error.message
      )
    }
  }

  if (loading) {
    return (
        <div className="loading">
          Carregando dashboard...
        </div>
    )
  }

  return (
      <main className="dashboard">

        <input
            ref={fileInputRef}
            className="hidden-file-input"
            type="file"
            accept=".xlsx,.xls"
            onChange={
              handleFileSelected
            }
        />

        <header className="topbar">

          <div>

            <p className="eyebrow">
              Cartório de Registro de Imóveis
            </p>

            <h1>
              Dashboard de Prazos e Demandas
            </h1>

            <p className="subtitle">
              Acompanhamento e priorização das demandas.
            </p>

          </div>

          <div className="topbar-actions">

            <button
                className="clear-button"
                onClick={
                  clearDashboard
                }
            >
              Limpar dashboard
            </button>

            <button
                className="import-button"
                style={{
                  background: '#3ccb8e',
                  borderColor: '#3ccb8e',
                  color: '#06251a',
                }}
                onClick={
                  exportExcel
                }
            >
              Exportar Excel
            </button>

            <button
                className="import-button"
                onClick={
                  openFileSelector
                }
            >
              Importar relatório
            </button>

          </div>

        </header>

        <section
            className="last-import"
            style={{
              marginBottom: '18px',
            }}
        >

          <div className="last-import-main">

            <span className="last-import-label">
              Visualização
            </span>

            <strong>
              {selectedViewDateTime
                  ? `Histórico de ${formatSnapshotDateTime(
                      selectedViewDateTime
                  )}`
                  : 'Situação atual'}
            </strong>

            <span className="last-import-file">
              {selectedViewDateTime
                  ? 'Fotografia salva das demandas nessa data e hora.'
                  : 'Dados mais recentes importados.'}
            </span>

          </div>

          <div
              style={{
                minWidth: '250px',
                display: 'flex',
                alignItems: 'center',
              }}
          >

            <select
                value={selectedViewDateTime}
                onChange={
                  handleViewDateTimeChange
                }
                disabled={viewLoading}
                style={{
                  width: '100%',
                  boxSizing: 'border-box',
                  padding: '10px 12px',
                  borderRadius: '8px',
                  border: '1px solid rgba(255,255,255,0.12)',
                  background: '#071b36',
                  color: '#ffffff',
                  font: 'inherit',
                  cursor: viewLoading
                      ? 'wait'
                      : 'pointer',
                }}
            >

              <option value="">
                Situação atual
              </option>

              {historyDateTimes.map(
                  (dateTime) => (

                      <option
                          key={dateTime}
                          value={dateTime}
                      >
                        {formatSnapshotDateTime(
                            dateTime
                        )}
                      </option>

                  )
              )}

            </select>

          </div>

        </section>

        {lastImport && (

            <section className="last-import">

              <div className="last-import-main">

                <span className="last-import-label">
                  Última importação
                </span>

                <strong>
                  {formatDateTime(
                      lastImport.importedAt
                  )}
                </strong>

                <span className="last-import-file">
                  {lastImport.fileName}
                </span>

              </div>

              <div className="last-import-stats">

                <span>
                  Processados
                  <strong>
                    {lastImport.processed}
                  </strong>
                </span>

                <span>
                  Importados
                  <strong>
                    {lastImport.imported}
                  </strong>
                </span>

                <span>
                  Atualizados
                  <strong>
                    {lastImport.updated}
                  </strong>
                </span>

                <span>
                  Ignorados
                  <strong>
                    {lastImport.skipped}
                  </strong>
                </span>

                <span>
                  Erros
                  <strong>
                    {lastImport.errors}
                  </strong>
                </span>

              </div>

            </section>

        )}

        <section className="cards">

          <div className="card">
            <span>Total de demandas</span>
            <strong>
              {summary?.total ?? 0}
            </strong>
          </div>

          <div className="card danger">
            <span>
              Qualificações vencidas
            </span>
            <strong>
              {summary?.overdue ?? 0}
            </strong>
          </div>

          <div className="card today">
            <span>
              Qualifica hoje
            </span>
            <strong>
              {summary?.today ?? 0}
            </strong>
          </div>

          <div className="card warning">
            <span>
              Qualificações próximas
            </span>
            <strong>
              {summary?.dueSoon ?? 0}
            </strong>
          </div>

          <div className="card success">
            <span>
              Qualificações futuras
            </span>
            <strong>
              {summary?.onTime ?? 0}
            </strong>
          </div>

          <div className="card completed">
            <span>
              Concluídas
            </span>
            <strong>
              {summary?.completed ?? 0}
            </strong>
          </div>

          <div className="card">
            <span>
              Sem qualificação
            </span>
            <strong>
              {summary?.noDeadline ?? 0}
            </strong>
          </div>

        </section>

        <section className="charts-grid">

          <div className="panel chart-panel">

            <div className="panel-title">

              <p className="eyebrow">
                Visão geral
              </p>

              <h2>
                Situação da qualificação
              </h2>

            </div>

            <div className="chart-area">

              <ResponsiveContainer
                  width="100%"
                  height="100%"
              >

                <PieChart>

                  <Pie
                      data={
                        qualificationDistribution
                      }
                      dataKey="count"
                      nameKey="category"
                      innerRadius={65}
                      outerRadius={100}
                      paddingAngle={4}
                      stroke="none"
                      animationDuration={
                        700
                      }
                  >

                    {qualificationDistribution.map(
                        (entry, index) => (
                            <Cell
                                key={
                                  entry.category
                                }
                                fill={
                                  QUALIFICATION_COLORS[
                                  index %
                                  QUALIFICATION_COLORS.length
                                      ]
                                }
                                stroke="none"
                            />
                        )
                    )}

                  </Pie>

                  <Tooltip
                      formatter={(
                          value,
                          name
                      ) => [
                        `${value} demanda${value === 1 ? '' : 's'}`,
                        name,
                      ]}
                      contentStyle={
                        tooltipStyle
                      }
                  />

                </PieChart>

              </ResponsiveContainer>

            </div>

            <div className="legend">

              {qualificationDistribution.map(
                  (item, index) => (

                      <div
                          className="legend-item"
                          key={
                            item.category
                          }
                      >

                        <span
                            className="legend-color"
                            style={{
                              background:
                                  QUALIFICATION_COLORS[
                                  index %
                                  QUALIFICATION_COLORS.length
                                      ],
                            }}
                        />

                        <span>
                          {item.category}
                        </span>

                        <strong>
                          {item.count}
                        </strong>

                      </div>

                  )
              )}

            </div>

          </div>

          <div className="panel chart-panel">

            <div className="panel-title">

              <p className="eyebrow">
                Qualificação
              </p>

              <h2>
                Demandas por dias de atraso da qualificação
              </h2>

            </div>

            <div className="chart-area">

              <ResponsiveContainer
                  width="100%"
                  height="100%"
              >

                <BarChart
                    data={
                      delayRanges
                    }
                    margin={{
                      top: 20,
                      right: 10,
                      left: 0,
                      bottom: 0,
                    }}
                >

                  <CartesianGrid
                      strokeDasharray="3 3"
                      stroke="rgba(255,255,255,0.06)"
                      vertical={false}
                  />

                  <XAxis
                      dataKey="range"
                      stroke="#748ba8"
                      tickLine={false}
                  />

                  <YAxis
                      allowDecimals={false}
                      stroke="#748ba8"
                      tickLine={false}
                      axisLine={false}
                  />

                  <Tooltip
                      cursor={false}
                      labelFormatter={
                        (label) =>
                            `Atraso: ${label}`
                      }
                      formatter={
                        (value) => [
                          `${value} demanda${value === 1 ? '' : 's'}`,
                          'Demandas atrasadas',
                        ]
                      }
                      contentStyle={
                        tooltipStyle
                      }
                  />

                  <Bar
                      dataKey="count"
                      name="Demandas atrasadas"
                      label={{
                        position: 'top',
                        fill: '#ffffff',
                      }}
                      fill="#1494ff"
                      stroke="none"
                      radius={[
                        8,
                        8,
                        0,
                        0,
                      ]}
                      animationDuration={
                        700
                      }
                      activeBar={{
                        fillOpacity:
                            0.82,
                        stroke:
                            'none',
                      }}
                  />

                </BarChart>

              </ResponsiveContainer>

            </div>

          </div>

          <div className="panel chart-panel">

            <div className="panel-title">

              <p className="eyebrow">
                Distribuição
              </p>

              <h2>
                Demandas por grupo operacional
              </h2>

            </div>

            <div className="chart-area">

              <ResponsiveContainer
                  width="100%"
                  height="100%"
              >

                <BarChart
                    data={
                      sectorDistribution
                    }
                >

                  <CartesianGrid
                      strokeDasharray="3 3"
                      stroke="rgba(255,255,255,0.06)"
                      vertical={false}
                  />

                  <XAxis
                      dataKey="category"
                      stroke="#748ba8"
                      tickLine={false}
                      tickFormatter={
                        formatCategory
                      }
                  />

                  <YAxis
                      allowDecimals={
                        false
                      }
                      stroke="#748ba8"
                      tickLine={false}
                      axisLine={false}
                  />

                  <Tooltip
                      cursor={false}
                      labelFormatter={
                        formatCategory
                      }
                      formatter={(
                          value
                      ) => [
                        value,
                        'Quantidade',
                      ]}
                      contentStyle={
                        tooltipStyle
                      }
                  />

                  <Bar
                      dataKey="count"
                      name="Quantidade"
                      fill="#14d9b1"
                      stroke="none"
                      radius={[
                        8,
                        8,
                        0,
                        0,
                      ]}
                      animationDuration={
                        700
                      }
                      activeBar={{
                        fillOpacity:
                            0.82,
                        stroke:
                            'none',
                      }}
                  />

                </BarChart>

              </ResponsiveContainer>

            </div>

          </div>

          <div className="panel chart-panel">

            <div className="panel-title">

              <p className="eyebrow">
                Distribuição
              </p>

              <h2>
                Demandas por serviço
              </h2>

            </div>

            <div className="chart-area">

              <ResponsiveContainer
                  width="100%"
                  height="100%"
              >

                <BarChart
                    data={
                      typeDistribution
                    }
                >

                  <CartesianGrid
                      strokeDasharray="3 3"
                      stroke="rgba(255,255,255,0.06)"
                      vertical={false}
                  />

                  <XAxis
                      dataKey="category"
                      stroke="#748ba8"
                      tickLine={false}
                  />

                  <YAxis
                      allowDecimals={
                        false
                      }
                      stroke="#748ba8"
                      tickLine={false}
                      axisLine={false}
                  />

                  <Tooltip
                      cursor={false}
                      formatter={(
                          value
                      ) => [
                        value,
                        'Quantidade',
                      ]}
                      contentStyle={
                        tooltipStyle
                      }
                  />

                  <Bar
                      dataKey="count"
                      name="Quantidade"
                      fill="#8b5cf6"
                      stroke="none"
                      radius={[
                        8,
                        8,
                        0,
                        0,
                      ]}
                      animationDuration={
                        700
                      }
                      activeBar={{
                        fillOpacity:
                            0.82,
                        stroke:
                            'none',
                      }}
                  />

                </BarChart>

              </ResponsiveContainer>

            </div>

          </div>

        </section>

        <section className="panel critical-panel">

          <div className="panel-header">

            <div>

              <p className="eyebrow">
                Prioridade
              </p>

              <h2>
                Demandas prioritárias
              </h2>

            </div>

            <span>
              {criticalDemands.length}{' '}
              vencidas ou para hoje
            </span>

          </div>

          <div className="table-wrapper">

            <table>

              <thead>

              <tr>
                <th>Código</th>
                <th>Serviço</th>
                <th>Grupo operacional</th>
                <th>Qualificação</th>
                <th>Prioridade</th>
                <th>Status</th>
              </tr>

              </thead>

              <tbody>

              {criticalDemands.length === 0 ? (

                  <tr>
                    <td
                        colSpan="6"
                        className="empty-row"
                    >
                      Nenhuma demanda prioritária.
                    </td>
                  </tr>

              ) : (

                  criticalDemands.map(
                      (demand) => (

                          <tr
                              key={
                                demand.id
                              }
                          >

                            <td className="protocol">
                              {
                                demand.externalId
                              }
                            </td>

                            <td>
                              {
                                demand.type
                              }
                            </td>

                            <td>
                              {formatCategory(
                                  demand.sector
                              )}
                            </td>

                            <td>
                              {formatDate(
                                  demand.deadline
                              )}
                            </td>

                            <td>

                              <span className="delay">
                                {
                                  demand.daysOverdue === 0
                                      ? 'Hoje'
                                      : `${demand.daysOverdue} dias`
                                }
                              </span>

                            </td>

                            <td>
                              {formatCategory(
                                  demand.status
                              )}
                            </td>

                          </tr>

                      )
                  )

              )}

              </tbody>

            </table>

          </div>

        </section>

        <section className="panel all-demands-panel">

          <div className="panel-header">

            <div>

              <p className="eyebrow">
                Consulta
              </p>

              <h2>
                Todas as demandas
              </h2>

            </div>

            <span>
              {
                filteredDemands.length
              }{' '}
              de{' '}
              {
                allDemands.length
              }
            </span>

          </div>

          <div className="filters-grid">

            <div className="filter-control search-control">

              <label>
                Buscar código
              </label>

              <input
                  type="text"
                  placeholder="Ex.: 12345"
                  value={search}
                  onChange={
                    (event) =>
                        setSearch(
                            event.target.value
                        )
                  }
              />

            </div>

            <div className="filter-control">

              <label>
                Serviço
              </label>

              <select
                  value={
                    typeFilter
                  }
                  onChange={
                    (event) =>
                        setTypeFilter(
                            event.target.value
                        )
                  }
              >

                <option value="">
                  Todos
                </option>

                {availableTypes.map(
                    (type) => (

                        <option
                            key={type}
                            value={type}
                        >
                          {type}
                        </option>

                    )
                )}

              </select>

            </div>

            <div className="filter-control">

              <label>
                Grupo operacional
              </label>

              <select
                  value={
                    sectorFilter
                  }
                  onChange={
                    (event) =>
                        setSectorFilter(
                            event.target.value
                        )
                  }
              >

                <option value="">
                  Todos
                </option>

                {availableSectors.map(
                    (sector) => (

                        <option
                            key={sector}
                            value={sector}
                        >
                          {formatCategory(
                              sector
                          )}
                        </option>

                    )
                )}

              </select>

            </div>

            <div className="filter-control">

              <label>
                Status
              </label>

              <select
                  value={
                    statusFilter
                  }
                  onChange={
                    (event) =>
                        setStatusFilter(
                            event.target.value
                        )
                  }
              >

                <option value="">
                  Todos
                </option>

                {availableStatuses.map(
                    (status) => (

                        <option
                            key={status}
                            value={status}
                        >
                          {formatCategory(
                              status
                          )}
                        </option>

                    )
                )}

              </select>

            </div>

            <div className="filter-control">

              <label>
                Situação da qualificação
              </label>

              <select
                  value={
                    situationFilter
                  }
                  onChange={
                    (event) =>
                        setSituationFilter(
                            event.target.value
                        )
                  }
              >

                <option value="">
                  Todas
                </option>

                <option value="overdue">
                  Qualificações vencidas
                </option>

                <option value="today">
                  Qualifica hoje
                </option>

                <option value="due-soon">
                  Qualificações próximas
                </option>

                <option value="on-time">
                  Qualificações futuras
                </option>

                <option value="completed">
                  Concluídas
                </option>

                <option value="no-deadline">
                  Sem qualificação
                </option>

              </select>

            </div>

            <div className="filter-button-area">

              <button
                  className="clear-filters-button"
                  onClick={
                    clearFilters
                  }
              >
                Limpar filtros
              </button>

            </div>

          </div>

          <div className="results-info">

            Mostrando{' '}

            <strong>
              {
                filteredDemands.length
              }
            </strong>{' '}

            demanda(s).

          </div>

          <div className="table-wrapper">

            <table className="demands-table">

              <thead>

              <tr>
                <th>Código</th>
                <th>Serviço</th>
                <th>Etapa</th>
                <th>Responsável atual</th>
                <th>Cadastro</th>
                <th>Qualificação</th>
                <th>Vencimento</th>
                <th>Reingresso</th>
                <th>Situação</th>
                <th>Status</th>
              </tr>

              </thead>

              <tbody>

              {filteredDemands.length === 0 ? (

                  <tr>
                    <td
                        colSpan="10"
                        className="empty-row"
                    >
                      Nenhuma demanda encontrada.
                    </td>
                  </tr>

              ) : (

                  filteredDemands.map(
                      (demand) => {

                        const situation =
                            calculateSituation(
                                demand,
                                selectedViewDateTime
                                    ? getDateFromDateTime(
                                        selectedViewDateTime
                                    )
                                    : getTodayInputDate()
                            )

                        return (

                            <tr
                                key={
                                  demand.id
                                }
                            >

                              <td className="protocol">
                                {
                                    demand.externalId ||
                                    '-'
                                }
                              </td>

                              <td>
                                {
                                    demand.type ||
                                    '-'
                                }
                              </td>

                              <td>
                                {
                                    demand.stage ||
                                    '-'
                                }
                              </td>

                              <td>
                                {
                                    demand.responsible ||
                                    '-'
                                }
                              </td>

                              <td>
                                {formatDate(
                                    demand.entryDate
                                )}
                              </td>

                              <td>
                                {formatDate(
                                    demand.qualificationDate
                                )}
                              </td>

                              <td>
                                {formatDate(
                                    demand.deadline
                                )}
                              </td>

                              <td>
                                {formatDate(
                                    demand.reentryDate
                                )}
                              </td>

                              <td>

                                <span
                                    className={`situation-badge ${situation}`}
                                >
                                  {situationLabel(
                                      situation
                                  )}
                                </span>

                              </td>

                              <td>
                                {formatCategory(
                                    demand.status
                                )}
                              </td>

                            </tr>

                        )
                      }
                  )

              )}

              </tbody>

            </table>

          </div>

        </section>

        {importOpen && (

            <div className="modal-overlay">

              <div className="import-modal">

                <div className="modal-header">

                  <div>

                    <p className="eyebrow">
                      Importação
                    </p>

                    <h2>
                      Importar relatório Excel
                    </h2>

                  </div>

                  <button
                      className="close-button"
                      onClick={
                        closeImport
                      }
                  >
                    ×
                  </button>

                </div>

                <div className="file-info">

                  <span>
                    Arquivo selecionado
                  </span>

                  <strong>
                    {
                      selectedFile?.name
                    }
                  </strong>

                </div>

                <div
                    className="file-info"
                    style={{
                      marginTop: '12px',
                      alignItems: 'flex-start',
                    }}
                >

                  <label
                      style={{
                        width: '100%',
                      }}
                  >

                    <span
                        style={{
                          display: 'block',
                          marginBottom: '8px',
                        }}
                    >
                      Data e hora da situação
                    </span>

                    <input
                        type="datetime-local"
                        step="60"
                        value={
                          referenceDateTime
                        }
                        onChange={
                          (event) =>
                              setReferenceDateTime(
                                  event.target.value
                              )
                        }
                        style={{
                          width: '100%',
                          maxWidth: '240px',
                          boxSizing: 'border-box',
                          padding: '10px 12px',
                          borderRadius: '8px',
                          border: '1px solid rgba(255,255,255,0.12)',
                          background: '#071b36',
                          color: '#ffffff',
                          font: 'inherit',
                        }}
                    />

                    <small
                        style={{
                          display: 'block',
                          marginTop: '8px',
                          color: '#8fa6c3',
                        }}
                    >
                      Importações com a mesma data e hora atualizam a mesma fotografia sem duplicar o código.
                    </small>

                  </label>

                </div>

                {importError && (

                    <div className="import-message error">
                      {importError}
                    </div>

                )}

                {importResult && (

                    <div className="import-message success-message">

                      <strong>
                        Importação concluída.
                      </strong>

                      <span>
                        Processados:{' '}
                        {
                          importResult.processed
                        }
                      </span>

                      <span>
                        Importados:{' '}
                        {
                          importResult.imported
                        }
                      </span>

                      <span>
                        Atualizados:{' '}
                        {
                          importResult.updated
                        }
                      </span>

                      <span>
                        Ignorados:{' '}
                        {
                          importResult.skipped
                        }
                      </span>

                    </div>

                )}

                {!preview &&
                    !importError && (

                        <div className="preview-loading">
                          Lendo arquivo...
                        </div>

                    )}

                {preview &&
                    !importResult && (
                        <>

                          <div className="import-section">

                            <div className="import-section-title">

                              <h3>
                                Mapeamento das colunas
                              </h3>

                              <p>
                                O sistema tenta mapear automaticamente. Você também pode escolher manualmente uma coluna compatível com cada campo.
                              </p>

                            </div>

                            <div className="mapping-grid">

                              {mappingFields.map(
                                  (field) => {

                                    const availableHeaders =
                                        getAvailableHeaders(
                                            field,
                                            preview.headers
                                        )

                                    return (

                                        <label
                                            className="mapping-field"
                                            key={
                                              field.key
                                            }
                                        >

                                          <span>
                                            {
                                              field.label
                                            }
                                          </span>

                                          <select
                                              value={
                                                  mapping[
                                                      field.key
                                                      ] ?? ''
                                              }
                                              onChange={
                                                (event) =>
                                                    changeMapping(
                                                        field.key,
                                                        event.target.value
                                                    )
                                              }
                                          >

                                            <option value="">
                                              Não mapear
                                            </option>

                                            {availableHeaders.map(
                                                (header) => (

                                                    <option
                                                        key={
                                                          header
                                                        }
                                                        value={
                                                          header
                                                        }
                                                    >
                                                      {
                                                        header
                                                      }
                                                    </option>

                                                )
                                            )}

                                          </select>

                                        </label>

                                    )
                                  }
                              )}

                            </div>

                          </div>

                          <div className="import-section">

                            <div className="import-section-title">

                              <h3>
                                Prévia do arquivo
                              </h3>

                              <p>
                                Primeiras linhas encontradas no relatório.
                              </p>

                            </div>

                            <div className="preview-table-wrapper">

                              <table className="preview-table">

                                <thead>

                                <tr>

                                  {preview.headers.map(
                                      (header) => (

                                          <th
                                              key={
                                                header
                                              }
                                          >
                                            {
                                              header
                                            }
                                          </th>

                                      )
                                  )}

                                </tr>

                                </thead>

                                <tbody>

                                {preview.rows.map(
                                    (
                                        row,
                                        rowIndex
                                    ) => (

                                        <tr
                                            key={
                                              rowIndex
                                            }
                                        >

                                          {preview.headers.map(
                                              (
                                                  header,
                                                  columnIndex
                                              ) => (

                                                  <td
                                                      key={`${header}-${columnIndex}`}
                                                  >
                                                    {
                                                        row[
                                                            columnIndex
                                                            ] || '-'
                                                    }
                                                  </td>

                                              )
                                          )}

                                        </tr>

                                    )
                                )}

                                </tbody>

                              </table>

                            </div>

                          </div>

                          <div className="modal-actions">

                            <button
                                className="secondary-button"
                                onClick={
                                  closeImport
                                }
                            >
                              Cancelar
                            </button>

                            <button
                                className="confirm-button"
                                onClick={
                                  confirmImport
                                }
                                disabled={
                                  importing
                                }
                            >
                              {
                                importing
                                    ? 'Importando...'
                                    : 'Confirmar importação'
                              }
                            </button>

                          </div>

                        </>
                    )}

                {importResult && (

                    <div className="modal-actions">

                      <button
                          className="confirm-button"
                          onClick={
                            closeImport
                          }
                      >
                        Fechar
                      </button>

                    </div>

                )}

              </div>

            </div>

        )}

      </main>
  )
}

export default App