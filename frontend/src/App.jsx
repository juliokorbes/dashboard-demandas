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

const COLORS = [
  '#1494ff',
  '#14d9b1',
  '#8b5cf6',
  '#f4b942',
  '#ff6577',
  '#4fd1ff',
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
    label: 'Identificador / Protocolo',
    aliases: [
      'protocolo',
      'numero protocolo',
      'numero do protocolo',
      'n protocolo',
      'id',
      'identificador',
    ],
  },
  {
    key: 'type',
    label: 'Tipo',
    aliases: [
      'tipo',
      'tipo ato',
      'tipo de ato',
      'categoria',
      'natureza',
      'servico',
      'serviço',
      'ato',
      'especie',
      'espécie',
    ],
  },
  {
    key: 'sector',
    label: 'Setor',
    aliases: [
      'setor',
      'departamento',
      'area',
      'área',
      'unidade',
      'equipe',
    ],
  },
  {
    key: 'responsible',
    label: 'Responsável',
    aliases: [
      'responsavel',
      'responsável',
      'atendente',
      'usuario',
      'usuário',
      'servidor',
      'colaborador',
      'analista',
    ],
  },
  {
    key: 'entryDate',
    label: 'Data de entrada',
    aliases: [
      'data de entrada',
      'data entrada',
      'entrada',
      'recebimento',
      'data de recebimento',
      'abertura',
      'data de abertura',
    ],
  },
  {
    key: 'deadline',
    label: 'Prazo',
    aliases: [
      'prazo',
      'data prazo',
      'prazo final',
      'data limite',
      'vencimento',
      'data de vencimento',
      'deadline',
    ],
  },
  {
    key: 'status',
    label: 'Status',
    aliases: [
      'status',
      'situacao',
      'situação',
      'estado',
      'andamento',
    ],
  },
  {
    key: 'description',
    label: 'Descrição / Observação',
    aliases: [
      'descricao',
      'descrição',
      'observacao',
      'observação',
      'assunto',
      'detalhes',
      'nota',
      'notas',
      'informacoes',
      'informações',
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
    headers,
    mapping
) {
  const usedHeaders =
      Object.entries(mapping)
          .filter(([key, value]) => {
            return (
                key !== field.key &&
                value
            )
          })
          .map(([, value]) => value)

  return headers.filter((header) => {
    return (
        !usedHeaders.includes(header) &&
        isCompatibleHeader(
            field,
            header
        )
    )
  })
}

function createAutomaticMapping(headers) {
  const result = {}

  const usedHeaders =
      new Set()

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
      .replace(
          /\b\w/g,
          (letter) =>
              letter.toUpperCase()
      )
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

function isCompleted(status) {
  return (
      normalizeText(status) ===
      'concluida'
  )
}

function calculateSituation(demand) {
  if (isCompleted(demand.status)) {
    return 'completed'
  }

  if (!demand.deadline) {
    return 'no-deadline'
  }

  const today = new Date()

  today.setHours(
      0,
      0,
      0,
      0
  )

  const deadline =
      new Date(
          `${demand.deadline}T00:00:00`
      )

  const difference =
      Math.round(
          (deadline - today) /
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

  if (difference <= 5) {
    return 'due-soon'
  }

  return 'on-time'
}

function situationLabel(situation) {
  const labels = {
    overdue: 'Atrasada',
    'due-soon':
        'Próxima do prazo',
    'on-time':
        'Dentro do prazo',
    completed:
        'Concluída',
    'no-deadline':
        'Sem prazo',
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
    statusDistribution,
    setStatusDistribution,
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
                    '/dashboard/status-distribution'
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
              ])

          const [
            summaryResponse,
            criticalResponse,
            delayResponse,
            statusResponse,
            sectorResponse,
            typeResponse,
            demandsResponse,
            historyResponse,
          ] = responses

          const requiredResponses = [
            summaryResponse,
            criticalResponse,
            delayResponse,
            statusResponse,
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
            statusData,
            sectorData,
            typeData,
            demandsData,
          ] =
              await Promise.all([
                summaryResponse.json(),
                criticalResponse.json(),
                delayResponse.json(),
                statusResponse.json(),
                sectorResponse.json(),
                typeResponse.json(),
                demandsResponse.json(),
              ])

          setSummary(
              summaryData
          )

          setCriticalDemands(
              criticalData
          )

          setDelayRanges(
              delayData
          )

          setStatusDistribution(
              statusData
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
                      demand
                  ) !== situationFilter
              ) {
                return false
              }

              return true
            })
            .sort((a, b) => {

              if (
                  !a.deadline &&
                  !b.deadline
              ) {
                return 0
              }

              if (!a.deadline) {
                return 1
              }

              if (!b.deadline) {
                return -1
              }

              return (
                  a.deadline.localeCompare(
                      b.deadline
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
          'Selecione uma coluna para Identificador / Protocolo.'
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

    if (
        fileInputRef.current
    ) {
      fileInputRef.current.value = ''
    }
  }

  /*
   * Limpa demandas e histórico
   * usando uma única operação no backend.
   */
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
                onClick={
                  openFileSelector
                }
            >
              Importar relatório
            </button>

          </div>

        </header>

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
              Duplicados
              <strong>
                {lastImport.duplicates}
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
            <span>Atrasadas</span>
            <strong>
              {summary?.overdue ?? 0}
            </strong>
          </div>

          <div className="card warning">
            <span>Próximas do prazo</span>
            <strong>
              {summary?.dueSoon ?? 0}
            </strong>
          </div>

          <div className="card success">
            <span>Dentro do prazo</span>
            <strong>
              {summary?.onTime ?? 0}
            </strong>
          </div>

          <div className="card completed">
            <span>Concluídas</span>
            <strong>
              {summary?.completed ?? 0}
            </strong>
          </div>

          <div className="card">
            <span>Sem prazo</span>
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
                Situação das demandas
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
                        statusDistribution
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

                    {statusDistribution.map(
                        (entry, index) => (
                            <Cell
                                key={
                                  entry.category
                                }
                                fill={
                                  COLORS[
                                  index %
                                  COLORS.length
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
                        value,
                        formatCategory(
                            name
                        ),
                      ]}
                      contentStyle={
                        tooltipStyle
                      }
                  />

                </PieChart>

              </ResponsiveContainer>

            </div>

            <div className="legend">

              {statusDistribution.map(
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
                            COLORS[
                            index %
                            COLORS.length
                                ],
                      }}
                  />

                        <span>
                    {formatCategory(
                        item.category
                    )}
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
                Prazos
              </p>

              <h2>
                Faixas de atraso
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
                Demandas por setor
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
                Demandas por tipo
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
                Demandas mais críticas
              </h2>

            </div>

            <span>
            {criticalDemands.length}{' '}
              atrasadas
          </span>

          </div>

          <div className="table-wrapper">

            <table>

              <thead>

              <tr>
                <th>Protocolo</th>
                <th>Tipo</th>
                <th>Setor</th>
                <th>Prazo</th>
                <th>Atraso</th>
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
                      Nenhuma demanda crítica.
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
                              {
                                demand.sector
                              }
                            </td>

                            <td>
                              {formatDate(
                                  demand.deadline
                              )}
                            </td>

                            <td>
                        <span className="delay">
                          {
                            demand.daysOverdue
                          }{' '}
                          dias
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
                Buscar protocolo
              </label>

              <input
                  type="text"
                  placeholder="Ex.: PROTOCOLO-001"
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

              <label>Tipo</label>

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

              <label>Setor</label>

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
                          {sector}
                        </option>

                    )
                )}

              </select>

            </div>

            <div className="filter-control">

              <label>Status</label>

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
                Situação do prazo
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
                  Atrasadas
                </option>

                <option value="due-soon">
                  Próximas do prazo
                </option>

                <option value="on-time">
                  Dentro do prazo
                </option>

                <option value="completed">
                  Concluídas
                </option>

                <option value="no-deadline">
                  Sem prazo
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
                <th>Protocolo</th>
                <th>Tipo</th>
                <th>Setor</th>
                <th>Prazo</th>
                <th>Situação</th>
                <th>Status</th>
              </tr>

              </thead>

              <tbody>

              {filteredDemands.length === 0 ? (

                  <tr>
                    <td
                        colSpan="6"
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
                                demand
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
                                    demand.sector ||
                                    '-'
                                }
                              </td>

                              <td>
                                {formatDate(
                                    demand.deadline
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
                  Duplicados:{' '}
                        {
                          importResult.duplicates
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
                                Selecione a coluna do Excel correspondente a cada campo.
                              </p>

                            </div>

                            <div className="mapping-grid">

                              {mappingFields.map(
                                  (field) => {

                                    const availableHeaders =
                                        getAvailableHeaders(
                                            field,
                                            preview.headers,
                                            mapping
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
                                                    {row[
                                                        columnIndex
                                                        ] || '-'}
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
                              {importing
                                  ? 'Importando...'
                                  : 'Confirmar importação'}
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