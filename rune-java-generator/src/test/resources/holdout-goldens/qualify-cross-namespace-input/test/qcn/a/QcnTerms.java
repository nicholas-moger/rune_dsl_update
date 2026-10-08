package test.qcn.a;

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
import test.qcn.a.meta.QcnTermsMeta;

import static java.util.Optional.ofNullable;

/**
 * The root; a qualifier in namespace b takes it as input.
 * @version 0.0.0
 */
@RosettaDataType(value="QcnTerms", builder=QcnTerms.QcnTermsBuilderImpl.class, version="0.0.0")
@RuneDataType(value="QcnTerms", model="test", builder=QcnTerms.QcnTermsBuilderImpl.class, version="0.0.0")
public interface QcnTerms extends RosettaModelObject {

	QcnTermsMeta metaData = new QcnTermsMeta();

	/*********************** Getter Methods  ***********************/
	String getKind();
	BigDecimal getNotional();

	/*********************** Build Methods  ***********************/
	QcnTerms build();
	
	QcnTerms.QcnTermsBuilder toBuilder();
	
	static QcnTerms.QcnTermsBuilder builder() {
		return new QcnTerms.QcnTermsBuilderImpl();
	}

	/*********************** Utility Methods  ***********************/
	@Override
	default RosettaMetaData<? extends QcnTerms> metaData() {
		return metaData;
	}
	
	@Override
	@RuneAttribute("@type")
	default Class<? extends QcnTerms> getType() {
		return QcnTerms.class;
	}
	
	@Override
	default void process(RosettaPath path, Processor processor) {
		processor.processBasic(path.newSubPath("kind"), String.class, getKind(), this);
		processor.processBasic(path.newSubPath("notional"), BigDecimal.class, getNotional(), this);
	}
	

	/*********************** Builder Interface  ***********************/
	interface QcnTermsBuilder extends QcnTerms, RosettaModelObjectBuilder {
		QcnTerms.QcnTermsBuilder setKind(String kind);
		QcnTerms.QcnTermsBuilder setNotional(BigDecimal notional);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processor.processBasic(path.newSubPath("kind"), String.class, getKind(), this);
			processor.processBasic(path.newSubPath("notional"), BigDecimal.class, getNotional(), this);
		}
		

		QcnTerms.QcnTermsBuilder prune();
	}

	/*********************** Immutable Implementation of QcnTerms  ***********************/
	class QcnTermsImpl implements QcnTerms {
		private final String kind;
		private final BigDecimal notional;
		
		protected QcnTermsImpl(QcnTerms.QcnTermsBuilder builder) {
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
		public QcnTerms build() {
			return this;
		}
		
		@Override
		public QcnTerms.QcnTermsBuilder toBuilder() {
			QcnTerms.QcnTermsBuilder builder = builder();
			setBuilderFields(builder);
			return builder;
		}
		
		protected void setBuilderFields(QcnTerms.QcnTermsBuilder builder) {
			ofNullable(getKind()).ifPresent(builder::setKind);
			ofNullable(getNotional()).ifPresent(builder::setNotional);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			QcnTerms _that = getType().cast(o);
		
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
			return "QcnTerms {" +
				"kind=" + this.kind + ", " +
				"notional=" + this.notional +
			'}';
		}
	}

	/*********************** Builder Implementation of QcnTerms  ***********************/
	class QcnTermsBuilderImpl implements QcnTerms.QcnTermsBuilder {
	
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
		public QcnTerms.QcnTermsBuilder setKind(String _kind) {
			this.kind = _kind == null ? null : _kind;
			return this;
		}
		
		@RosettaAttribute("notional")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("notional")
		@Override
		public QcnTerms.QcnTermsBuilder setNotional(BigDecimal _notional) {
			this.notional = _notional == null ? null : _notional;
			return this;
		}
		
		@Override
		public QcnTerms build() {
			return new QcnTerms.QcnTermsImpl(this);
		}
		
		@Override
		public QcnTerms.QcnTermsBuilder toBuilder() {
			return this;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public QcnTerms.QcnTermsBuilder prune() {
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
		public QcnTerms.QcnTermsBuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			QcnTerms.QcnTermsBuilder o = (QcnTerms.QcnTermsBuilder) other;
			
			
			merger.mergeBasic(getKind(), o.getKind(), this::setKind);
			merger.mergeBasic(getNotional(), o.getNotional(), this::setNotional);
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			QcnTerms _that = getType().cast(o);
		
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
			return "QcnTermsBuilder {" +
				"kind=" + this.kind + ", " +
				"notional=" + this.notional +
			'}';
		}
	}
}
