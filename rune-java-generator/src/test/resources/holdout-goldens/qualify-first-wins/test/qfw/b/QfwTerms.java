package test.qfw.b;

import com.rosetta.model.lib.RosettaModelObject;
import com.rosetta.model.lib.RosettaModelObjectBuilder;
import com.rosetta.model.lib.annotations.Accessor;
import com.rosetta.model.lib.annotations.AccessorType;
import com.rosetta.model.lib.annotations.RosettaAttribute;
import com.rosetta.model.lib.annotations.RosettaDataType;
import com.rosetta.model.lib.annotations.RuneAttribute;
import com.rosetta.model.lib.annotations.RuneDataType;
import com.rosetta.model.lib.meta.RosettaMetaData;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.process.BuilderMerger;
import com.rosetta.model.lib.process.BuilderProcessor;
import com.rosetta.model.lib.process.Processor;
import java.math.BigDecimal;
import java.util.Objects;
import test.qfw.b.meta.QfwTermsMeta;

import static java.util.Optional.ofNullable;

/**
 * The same-named root type in the LATER namespace - its configuration loses the first-wins race.
 * @version 0.0.0
 */
@RosettaDataType(value="QfwTerms", builder=QfwTerms.QfwTermsBuilderImpl.class, version="0.0.0")
@RuneDataType(value="QfwTerms", model="test", builder=QfwTerms.QfwTermsBuilderImpl.class, version="0.0.0")
public interface QfwTerms extends RosettaModelObject {

	QfwTermsMeta metaData = new QfwTermsMeta();

	/*********************** Getter Methods  ***********************/
	String getKind();
	BigDecimal getNotional();

	/*********************** Build Methods  ***********************/
	QfwTerms build();
	
	QfwTerms.QfwTermsBuilder toBuilder();
	
	static QfwTerms.QfwTermsBuilder builder() {
		return new QfwTerms.QfwTermsBuilderImpl();
	}

	/*********************** Utility Methods  ***********************/
	@Override
	default RosettaMetaData<? extends QfwTerms> metaData() {
		return metaData;
	}
	
	@Override
	@RuneAttribute("@type")
	default Class<? extends QfwTerms> getType() {
		return QfwTerms.class;
	}
	
	@Override
	default void process(RosettaPath path, Processor processor) {
		processor.processBasic(path.newSubPath("kind"), String.class, getKind(), this);
		processor.processBasic(path.newSubPath("notional"), BigDecimal.class, getNotional(), this);
	}
	

	/*********************** Builder Interface  ***********************/
	interface QfwTermsBuilder extends QfwTerms, RosettaModelObjectBuilder {
		QfwTerms.QfwTermsBuilder setKind(String kind);
		QfwTerms.QfwTermsBuilder setNotional(BigDecimal notional);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processor.processBasic(path.newSubPath("kind"), String.class, getKind(), this);
			processor.processBasic(path.newSubPath("notional"), BigDecimal.class, getNotional(), this);
		}
		

		QfwTerms.QfwTermsBuilder prune();
	}

	/*********************** Immutable Implementation of QfwTerms  ***********************/
	class QfwTermsImpl implements QfwTerms {
		private final String kind;
		private final BigDecimal notional;
		
		protected QfwTermsImpl(QfwTerms.QfwTermsBuilder builder) {
			this.kind = builder.getKind();
			this.notional = builder.getNotional();
		}
		
		@Override
		@RosettaAttribute("kind")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("kind")
		public String getKind() {
			return kind;
		}
		
		@Override
		@RosettaAttribute("notional")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("notional")
		public BigDecimal getNotional() {
			return notional;
		}
		
		@Override
		public QfwTerms build() {
			return this;
		}
		
		@Override
		public QfwTerms.QfwTermsBuilder toBuilder() {
			QfwTerms.QfwTermsBuilder builder = builder();
			setBuilderFields(builder);
			return builder;
		}
		
		protected void setBuilderFields(QfwTerms.QfwTermsBuilder builder) {
			ofNullable(getKind()).ifPresent(builder::setKind);
			ofNullable(getNotional()).ifPresent(builder::setNotional);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			QfwTerms _that = getType().cast(o);
		
			if (!Objects.equals(kind, _that.getKind())) return false;
			if (!Objects.equals(notional, _that.getNotional())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (kind != null ? kind.hashCode() : 0);
			_result = 31 * _result + (notional != null ? notional.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "QfwTerms {" +
				"kind=" + this.kind + ", " +
				"notional=" + this.notional +
			'}';
		}
	}

	/*********************** Builder Implementation of QfwTerms  ***********************/
	class QfwTermsBuilderImpl implements QfwTerms.QfwTermsBuilder {
	
		protected String kind;
		protected BigDecimal notional;
		
		@Override
		@RosettaAttribute("kind")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("kind")
		public String getKind() {
			return kind;
		}
		
		@Override
		@RosettaAttribute("notional")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("notional")
		public BigDecimal getNotional() {
			return notional;
		}
		
		@RosettaAttribute("kind")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("kind")
		@Override
		public QfwTerms.QfwTermsBuilder setKind(String _kind) {
			this.kind = _kind == null ? null : _kind;
			return this;
		}
		
		@RosettaAttribute("notional")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("notional")
		@Override
		public QfwTerms.QfwTermsBuilder setNotional(BigDecimal _notional) {
			this.notional = _notional == null ? null : _notional;
			return this;
		}
		
		@Override
		public QfwTerms build() {
			return new QfwTerms.QfwTermsImpl(this);
		}
		
		@Override
		public QfwTerms.QfwTermsBuilder toBuilder() {
			return this;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public QfwTerms.QfwTermsBuilder prune() {
			return this;
		}
		
		@Override
		public boolean hasData() {
			if (getKind()!=null) return true;
			if (getNotional()!=null) return true;
			return false;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public QfwTerms.QfwTermsBuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			QfwTerms.QfwTermsBuilder o = (QfwTerms.QfwTermsBuilder) other;
			
			
			merger.mergeBasic(getKind(), o.getKind(), this::setKind);
			merger.mergeBasic(getNotional(), o.getNotional(), this::setNotional);
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			QfwTerms _that = getType().cast(o);
		
			if (!Objects.equals(kind, _that.getKind())) return false;
			if (!Objects.equals(notional, _that.getNotional())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (kind != null ? kind.hashCode() : 0);
			_result = 31 * _result + (notional != null ? notional.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "QfwTermsBuilder {" +
				"kind=" + this.kind + ", " +
				"notional=" + this.notional +
			'}';
		}
	}
}
