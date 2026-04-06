import SwiftUI

struct PayoutView: View {
    @Environment(TournamentStore.self) private var store
    private let payoutPresets: [(String, [Int])] = [
        ("60/40", [60, 40]),
        ("50/30/20", [50, 30, 20]),
        ("70/20/10", [70, 20, 10]),
        ("40/30/20/10", [40, 30, 20, 10]),
    ]

    var body: some View {
        let state = store.state
        let payouts = PayoutCalculator.calculate(state)
        let percents = state.config.payoutPercents
        let totalPercent = percents.reduce(0, +)
        let isValidTotal = totalPercent == 100

        NavigationStack {
            ZStack {
                RTTheme.feltGreenDark.ignoresSafeArea()
                ScrollView {
                    VStack(alignment: .leading, spacing: 16) {
                        HStack {
                            SummaryPill(label: "분배 순위", value: "\(percents.count)명")
                            SummaryPill(label: "합계", value: "\(totalPercent)%", highlight: totalPercent == 100)
                        }

                        TotalPrizeCard(
                            pool: state.totalPrizePool,
                            gross: state.totalBuyInsGross,
                            fee: state.totalFee,
                            buyIn: state.config.buyInAmount,
                            feePerEntry: state.config.feePerEntry,
                            entries: state.totalBuyInsAndRebuys
                        )

                        HStack {
                            Text("분배 비율 (%)")
                                .font(.headline)
                                .foregroundStyle(.white)
                            Spacer()
                            Text("합계 \(totalPercent)%")
                                .fontWeight(.bold)
                                .foregroundStyle(totalPercent == 100 ? RTTheme.chipGold : RTTheme.chipRed)
                        }

                        Text(
                            isValidTotal
                            ? "분배 비율이 100%로 맞춰져 있습니다."
                            : "분배 비율 합계를 100%로 맞추면 상금이 정확하게 계산됩니다."
                        )
                        .font(.subheadline)
                        .foregroundStyle(isValidTotal ? RTTheme.chipGold : RTTheme.chipRed)

                        if state.isTournamentComplete {
                            VStack(alignment: .leading, spacing: 6) {
                                Text("게임 종료 요약")
                                    .font(.headline)
                                    .foregroundStyle(.white)
                                Text("우승: \(state.winner?.name ?? "미정")")
                                    .fontWeight(.bold)
                                    .foregroundStyle(RTTheme.chipGold)
                                ForEach(state.finalStandings.prefix(max(payouts.count, 3)), id: \.id) { player in
                                    let amount = player.placement.flatMap { place in
                                        payouts.indices.contains(place - 1) ? payouts[place - 1].amount : 0
                                    } ?? 0
                                    Text("\(player.placement ?? 0)위 \(player.name) · \(formatted(amount))원")
                                        .foregroundStyle(RTTheme.onSurfaceMuted)
                                }
                            }
                            .frame(maxWidth: .infinity, alignment: .leading)
                            .padding()
                            .background(RTTheme.surfaceElevated, in: RoundedRectangle(cornerRadius: 16))
                        }

                        VStack(alignment: .leading, spacing: 10) {
                            Text("프리셋")
                                .font(.headline)
                                .foregroundStyle(.white)
                            ForEach(Array(payoutPresets.chunked(into: 2).enumerated()), id: \.offset) { _, row in
                                HStack(spacing: 10) {
                                    ForEach(row, id: \.0) { preset in
                                        Button(preset.0) {
                                            store.updatePayoutPercents(preset.1)
                                        }
                                        .buttonStyle(.bordered)
                                        .tint(RTTheme.surfaceElevated)
                                        .frame(maxWidth: .infinity)
                                    }
                                }
                            }
                        }

                        ForEach(Array(percents.enumerated()), id: \.offset) { index, percent in
                            PayoutRow(
                                place: index + 1,
                                percent: percent,
                                amount: payouts.indices.contains(index) ? payouts[index].amount : 0,
                                canRemove: percents.count > 1,
                                onPercentChange: { newValue in
                                    var list = percents
                                    list[index] = newValue
                                    store.updatePayoutPercents(list)
                                },
                                onRemove: {
                                    var list = percents
                                    list.remove(at: index)
                                    store.updatePayoutPercents(list)
                                }
                            )
                        }

                        Button {
                            store.updatePayoutPercents(percents + [0])
                        } label: {
                            Label("순위 추가", systemImage: "plus.circle.fill")
                                .frame(maxWidth: .infinity)
                                .padding(.vertical, 8)
                        }
                        .buttonStyle(.borderedProminent)
                        .tint(RTTheme.chipGold)
                        .foregroundStyle(RTTheme.feltGreenDark)
                    }
                    .padding()
                }
            }
            .dismissKeyboardOnTap()
            .navigationTitle("상금")
            .toolbarColorScheme(.dark, for: .navigationBar)
        }
    }
}

private struct SummaryPill: View {
    let label: String
    let value: String
    var highlight: Bool = true

    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(label)
                .font(.caption)
                .foregroundStyle(RTTheme.onSurfaceMuted)
            Text(value)
                .font(.headline.bold())
                .foregroundStyle(highlight ? RTTheme.chipGold : RTTheme.chipRed)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(.horizontal, 14)
        .padding(.vertical, 12)
        .background(RTTheme.surfaceHighlight, in: RoundedRectangle(cornerRadius: 18))
    }
}

private struct TotalPrizeCard: View {
    let pool: Int
    let gross: Int
    let fee: Int
    let buyIn: Int
    let feePerEntry: Int
    let entries: Int

    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            Text("총 상금 (수수료 제외)")
                .foregroundStyle(RTTheme.onSurfaceMuted)
            Text("\(formatted(pool))원")
                .font(.system(size: 34, weight: .bold))
                .foregroundStyle(RTTheme.chipGold)
            Text("바이인 \(formatted(buyIn)) × \(entries)건 = \(formatted(gross))원")
                .font(.caption)
                .foregroundStyle(RTTheme.onSurfaceMuted)
            if feePerEntry > 0 {
                Text("수수료 \(formatted(feePerEntry)) × \(entries)건 = -\(formatted(fee))원")
                    .font(.caption)
                    .foregroundStyle(RTTheme.chipRed)
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding()
        .background(RTTheme.surfaceElevated, in: RoundedRectangle(cornerRadius: 16))
    }
}

private struct PayoutRow: View {
    let place: Int
    let percent: Int
    let amount: Int
    let canRemove: Bool
    let onPercentChange: (Int) -> Void
    let onRemove: () -> Void

    var body: some View {
        HStack {
            Text("\(place)위")
                .fontWeight(.bold)
                .foregroundStyle(.white)
                .frame(width: 40, alignment: .leading)

            TextField("%", value: Binding(get: { percent }, set: onPercentChange), format: .number)
                .keyboardType(.numberPad)
                .textFieldStyle(.roundedBorder)
                .frame(width: 80)

            Spacer()

            Text("\(formatted(amount))원")
                .foregroundStyle(RTTheme.chipGold)

            Button(action: onRemove) {
                Image(systemName: "minus.circle")
            }
            .buttonStyle(.borderless)
            .tint(RTTheme.chipRed)
            .disabled(!canRemove)
        }
        .padding(.horizontal, 12)
        .padding(.vertical, 10)
        .background(RTTheme.surfaceHighlight, in: RoundedRectangle(cornerRadius: 12))
    }
}

private func formatted(_ value: Int) -> String {
    let formatter = NumberFormatter()
    formatter.numberStyle = .decimal
    return formatter.string(from: NSNumber(value: value)) ?? "\(value)"
}

private extension Array {
    func chunked(into size: Int) -> [[Element]] {
        stride(from: 0, to: count, by: size).map { index in
            Array(self[index..<Swift.min(index + size, count)])
        }
    }
}
