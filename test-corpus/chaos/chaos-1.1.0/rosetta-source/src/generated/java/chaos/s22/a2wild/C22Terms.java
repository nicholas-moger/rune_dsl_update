package chaos.s22.a2wild;

import chaos.s22.a2wild.h.C22Aux;
import chaos.s22.a2wild.meta.C22TermsMeta;
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

import static java.util.Optional.ofNullable;

/**
 * The qualifiable root type.
 * @version 1.0.0
 */
@RosettaDataType(value="C22Terms", builder=C22Terms.C22TermsBuilderImpl.class, version="1.0.0")
@RuneDataType(value="C22Terms", model="chaos", builder=C22Terms.C22TermsBuilderImpl.class, version="1.0.0")
public interface C22Terms extends RosettaModelObject {

	C22TermsMeta metaData = new C22TermsMeta();

	/*********************** Getter Methods  ***********************/
	String getKind();
	BigDecimal getNotional();
	C22Aux getAux();

	/*********************** Build Methods  ***********************/
	C22Terms build();
	
	C22Terms.C22TermsBuilder toBuilder();
	
	static C22Terms.C22TermsBuilder builder() {
		return new C22Terms.C22TermsBuilderImpl();
	}

	/*********************** Utility Methods  ***********************/
	@Override
	default RosettaMetaData<? extends C22Terms> metaData() {
		return metaData;
	}
	
	@Override
	@RuneAttribute("@type")
	default Class<? extends C22Terms> getType() {
		return C22Terms.class;
	}
	
	@Override
	default void process(RosettaPath path, Processor processor) {
		processor.processBasic(path.newSubPath("kind"), String.class, getKind(), this);
		processor.processBasic(path.newSubPath("notional"), BigDecimal.class, getNotional(), this);
		processRosetta(path.newSubPath("aux"), processor, C22Aux.class, getAux());
	}
	

	/*********************** Builder Interface  ***********************/
	interface C22TermsBuilder extends C22Terms, RosettaModelObjectBuilder {
		C22Aux.C22AuxBuilder getOrCreateAux();
		@Override
		C22Aux.C22AuxBuilder getAux();
		C22Terms.C22TermsBuilder setKind(String kind);
		C22Terms.C22TermsBuilder setNotional(BigDecimal notional);
		C22Terms.C22TermsBuilder setAux(C22Aux aux);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processor.processBasic(path.newSubPath("kind"), String.class, getKind(), this);
			processor.processBasic(path.newSubPath("notional"), BigDecimal.class, getNotional(), this);
			processRosetta(path.newSubPath("aux"), processor, C22Aux.C22AuxBuilder.class, getAux());
		}
		

		C22Terms.C22TermsBuilder prune();
	}

	/*********************** Immutable Implementation of C22Terms  ***********************/
	class C22TermsImpl implements C22Terms {
		private final String kind;
		private final BigDecimal notional;
		private final C22Aux aux;
		
		protected C22TermsImpl(C22Terms.C22TermsBuilder builder) {
			this.kind = builder.getKind();
			this.notional = builder.getNotional();
			this.aux = ofNullable(builder.getAux()).map(f->f.build()).orElse(null);
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
		@RosettaAttribute("aux")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("aux")
		public C22Aux getAux() {
			return aux;
		}
		
		@Override
		public C22Terms build() {
			return this;
		}
		
		@Override
		public C22Terms.C22TermsBuilder toBuilder() {
			C22Terms.C22TermsBuilder builder = builder();
			setBuilderFields(builder);
			return builder;
		}
		
		protected void setBuilderFields(C22Terms.C22TermsBuilder builder) {
			ofNullable(getKind()).ifPresent(builder::setKind);
			ofNullable(getNotional()).ifPresent(builder::setNotional);
			ofNullable(getAux()).ifPresent(builder::setAux);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			C22Terms _that = getType().cast(o);
		
			if (!Objects.equals(kind, _that.getKind())) return false;
			if (!Objects.equals(notional, _that.getNotional())) return false;
			if (!Objects.equals(aux, _that.getAux())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (kind != null ? kind.hashCode() : 0);
			_result = 31 * _result + (notional != null ? notional.hashCode() : 0);
			_result = 31 * _result + (aux != null ? aux.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "C22Terms {" +
				"kind=" + this.kind + ", " +
				"notional=" + this.notional + ", " +
				"aux=" + this.aux +
			'}';
		}
	}

	/*********************** Builder Implementation of C22Terms  ***********************/
	class C22TermsBuilderImpl implements C22Terms.C22TermsBuilder {
	
		protected String kind;
		protected BigDecimal notional;
		protected C22Aux.C22AuxBuilder aux;
		
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
		@RosettaAttribute("aux")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("aux")
		public C22Aux.C22AuxBuilder getAux() {
			return aux;
		}
		
		@Override
		public C22Aux.C22AuxBuilder getOrCreateAux() {
			C22Aux.C22AuxBuilder result;
			if (aux!=null) {
				result = aux;
			}
			else {
				result = aux = C22Aux.builder();
			}
			
			return result;
		}
		
		@RosettaAttribute("kind")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("kind")
		@Override
		public C22Terms.C22TermsBuilder setKind(String _kind) {
			this.kind = _kind == null ? null : _kind;
			return this;
		}
		
		@RosettaAttribute("notional")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("notional")
		@Override
		public C22Terms.C22TermsBuilder setNotional(BigDecimal _notional) {
			this.notional = _notional == null ? null : _notional;
			return this;
		}
		
		@RosettaAttribute("aux")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("aux")
		@Override
		public C22Terms.C22TermsBuilder setAux(C22Aux _aux) {
			this.aux = _aux == null ? null : _aux.toBuilder();
			return this;
		}
		
		@Override
		public C22Terms build() {
			return new C22Terms.C22TermsImpl(this);
		}
		
		@Override
		public C22Terms.C22TermsBuilder toBuilder() {
			return this;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public C22Terms.C22TermsBuilder prune() {
			if (aux!=null && !aux.prune().hasData()) aux = null;
			return this;
		}
		
		@Override
		public boolean hasData() {
			if (getKind()!=null) return true;
			if (getNotional()!=null) return true;
			if (getAux()!=null && getAux().hasData()) return true;
			return false;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public C22Terms.C22TermsBuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			C22Terms.C22TermsBuilder o = (C22Terms.C22TermsBuilder) other;
			
			merger.mergeRosetta(getAux(), o.getAux(), this::setAux);
			
			merger.mergeBasic(getKind(), o.getKind(), this::setKind);
			merger.mergeBasic(getNotional(), o.getNotional(), this::setNotional);
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			C22Terms _that = getType().cast(o);
		
			if (!Objects.equals(kind, _that.getKind())) return false;
			if (!Objects.equals(notional, _that.getNotional())) return false;
			if (!Objects.equals(aux, _that.getAux())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (kind != null ? kind.hashCode() : 0);
			_result = 31 * _result + (notional != null ? notional.hashCode() : 0);
			_result = 31 * _result + (aux != null ? aux.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "C22TermsBuilder {" +
				"kind=" + this.kind + ", " +
				"notional=" + this.notional + ", " +
				"aux=" + this.aux +
			'}';
		}
	}
}
