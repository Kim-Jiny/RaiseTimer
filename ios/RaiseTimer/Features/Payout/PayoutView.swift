import SwiftUI

struct PayoutView: View {
    @Environment(TournamentStore.self) private var store

    var body: some View {
        let state = store.state
        let payouts = PayoutCalculator.calculate(state)
        let percents = state.config.payoutPercents
        let totalPercent = percents.reduce(0, +)

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

                        ForEach(Array(percents.enumerated()), id: \.offset) { index, percent in
                            PayoutRow(
                                place: index + 1,
                                percent: percent,
                                amount: payouts.indices.contains(index) ? payouts[index].amount : 0,
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
